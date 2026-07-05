package tech.lougon.profitly.analysis.infrastructure.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.infrastructure.client.BrapiAnalysisClient;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFundDividendsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFundIndicatorsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFundListResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFundNavHistoryResponse;
import tech.lougon.profitly.analysis.infrastructure.persistence.FundDividendEventJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FundDocumentJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FundIndicatorJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FundNavHistoryJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFundDividendEventRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFundDocumentRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFundIndicatorRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFundNavHistoryRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.JpaTickerRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.TickerJpaEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Full sync pipeline for listed funds (FIAGRO, FI-Infra, FIDC, FIP) from the
 * /api/v2/funds/* endpoints: catalog + monthly indicators + daily NAV history +
 * dividend events + raw documents (profile, portfolio and type-specific reports).
 */
@Component
public class FundSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(FundSyncScheduler.class);

    private static final List<String> ASSET_TYPES = List.of("fiagro", "fiinfra", "fidc", "fip");
    /** brapi caps the funds endpoints at 20 symbols per call. */
    private static final int BATCH_SIZE = 20;
    private static final String BACKFILL_START_DATE = "2020-01-01";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final BrapiAnalysisClient brapiClient;
    private final JpaFundIndicatorRepository fundRepo;
    private final JpaFundNavHistoryRepository navHistoryRepo;
    private final JpaFundDividendEventRepository dividendRepo;
    private final JpaFundDocumentRepository documentRepo;
    private final JpaTickerRepository tickerRepo;

    public FundSyncScheduler(BrapiAnalysisClient brapiClient,
                              JpaFundIndicatorRepository fundRepo,
                              JpaFundNavHistoryRepository navHistoryRepo,
                              JpaFundDividendEventRepository dividendRepo,
                              JpaFundDocumentRepository documentRepo,
                              JpaTickerRepository tickerRepo) {
        this.brapiClient = brapiClient;
        this.fundRepo = fundRepo;
        this.navHistoryRepo = navHistoryRepo;
        this.dividendRepo = dividendRepo;
        this.documentRepo = documentRepo;
        this.tickerRepo = tickerRepo;
    }

    @Value("${profitly.sync.on-startup:false}")
    private boolean syncOnStartup;

    // Dev-only flag while the funds screen is under construction — turn off when done
    @Value("${profitly.sync.funds-on-startup:false}")
    private boolean fundsSyncOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void syncOnStartup() {
        if (!syncOnStartup && !fundsSyncOnStartup) return;
        log.info("Running fund startup sync");
        syncAll();
    }

    @Scheduled(cron = "0 40 19 * * *", zone = "America/Sao_Paulo")
    public void syncAll() {
        // 1. Catalog per assetType — one /funds/list call each brings everything
        Map<String, List<String>> symbolsByType = new LinkedHashMap<>();
        for (String assetType : ASSET_TYPES) {
            List<BrapiFundListResponse.FundItem> funds = brapiClient.fetchFundList(assetType);
            if (funds.isEmpty()) {
                log.warn("Fund list returned 0 results for assetType={}", assetType);
                continue;
            }
            List<String> symbols = new ArrayList<>();
            for (BrapiFundListResponse.FundItem item : funds) {
                if (item.symbol() == null) continue;
                try {
                    saveCurrent(item, assetType);
                    upsertTicker(item, assetType);
                    symbols.add(item.symbol());
                } catch (Exception e) {
                    log.warn("Failed to save fund {}: {}", item.symbol(), e.getMessage());
                }
            }
            log.info("Funds synced for assetType={}: {}/{}", assetType, symbols.size(), funds.size());
            symbolsByType.put(assetType, symbols);
        }

        List<String> allSymbols = symbolsByType.values().stream().flatMap(List::stream).toList();
        if (allSymbols.isEmpty()) {
            log.warn("Fund sync aborted: no fund symbols");
            return;
        }
        log.info("Fund sync: {} funds across {} types", allSymbols.size(), symbolsByType.size());

        syncIndicators(allSymbols);
        syncNavHistories(allSymbols);
        syncDividends(allSymbols);
        refreshComputedYields(allSymbols);
        syncDocuments(allSymbols, symbolsByType);
        updateTickerDailyChanges(allSymbols);
        log.info("Fund sync complete");
    }

    // ── Catalog ──────────────────────────────────────────────────────────────

    private void saveCurrent(BrapiFundListResponse.FundItem item, String assetType) {
        FundIndicatorJpaEntity entity = fundRepo.findById(item.symbol())
                .orElseGet(() -> { var e = new FundIndicatorJpaEntity(); e.setSymbol(item.symbol()); return e; });

        entity.setName(item.name());
        entity.setLegalName(item.legalName());
        entity.setCnpj(item.cnpj());
        entity.setFundType(item.assetType() != null ? item.assetType() : assetType);
        entity.setB3Classification(item.b3Classification());
        entity.setIsin(item.isin());
        entity.setStatus(item.status());
        entity.setPrice(item.price());
        entity.setPriceToNav(item.priceToNav());
        entity.setNavPerShare(item.navPerShare());
        entity.setEquity(item.equity());
        entity.setTotalAssets(item.totalAssets());
        entity.setAdminName(item.administratorName());
        entity.setAdminCnpj(item.administratorCnpj());
        entity.setManagerName(item.managerName());
        entity.setManagerCnpj(item.managerCnpj());
        if (item.totalInvestors() != null) {
            entity.setTotalInvestors(item.totalInvestors().longValue());
        }
        entity.setSyncedAt(Instant.now());
        fundRepo.save(entity);
    }

    /** Upserts a fund into the tickers table so it appears in search. */
    private void upsertTicker(BrapiFundListResponse.FundItem item, String assetType) {
        TickerJpaEntity ticker = tickerRepo.findBySymbol(item.symbol())
                .orElseGet(TickerJpaEntity::new);

        String name = item.name() != null ? item.name() : item.symbol();
        String fundType = item.assetType() != null ? item.assetType().toLowerCase() : assetType;

        ticker.setSymbol(item.symbol());
        ticker.setName(name);
        ticker.setLongName(item.legalName() != null ? item.legalName() : name);
        ticker.setAssetType("fund");
        ticker.setSubType(fundType);
        ticker.setIsActive(true);
        if (item.price() != null) {
            ticker.setLastPrice(BigDecimal.valueOf(item.price()));
        }
        ticker.setSyncedAt(Instant.now());

        tickerRepo.save(ticker);
    }

    // ── Monthly indicators ───────────────────────────────────────────────────

    private void syncIndicators(List<String> symbols) {
        int updated = 0;
        for (List<String> batch : batches(symbols)) {
            List<BrapiFundIndicatorsResponse.FundIndicators> results =
                    brapiClient.fetchFundIndicators(String.join(",", batch));
            for (var ind : results) {
                FundIndicatorJpaEntity entity = fundRepo.findById(ind.symbol()).orElse(null);
                if (entity == null) continue;
                entity.setAsOfDate(normalizeDate(ind.asOfDate()));
                if (ind.price() != null) entity.setPrice(ind.price());
                if (ind.navPerShare() != null) entity.setNavPerShare(ind.navPerShare());
                if (ind.priceToNav() != null) entity.setPriceToNav(ind.priceToNav());
                if (ind.equity() != null) entity.setEquity(ind.equity());
                if (ind.totalAssets() != null) entity.setTotalAssets(ind.totalAssets());
                if (ind.totalInvestors() != null) entity.setTotalInvestors(ind.totalInvestors().longValue());
                entity.setDailyApplications(ind.dailyApplications());
                entity.setDailyRedemptions(ind.dailyRedemptions());
                entity.setSharesOutstanding(ind.sharesOutstanding());
                entity.setMonthlyReturn(ind.monthlyReturn());
                entity.setPatrimonialMonthlyReturn(ind.patrimonialMonthlyReturn());
                entity.setDividendYieldMonthly(ind.dividendYieldMonthly());
                entity.setSyncedAt(Instant.now());
                fundRepo.save(entity);
                updated++;
            }
        }
        log.info("Fund indicators updated: {}/{}", updated, symbols.size());
    }

    // ── Daily NAV history ────────────────────────────────────────────────────

    /**
     * Funds without stored history get a full backfill since 2020; the rest only
     * fetch the last 3 months. Both paths run in batches of up to 20 symbols.
     */
    private void syncNavHistories(List<String> symbols) {
        List<String> backfill = new ArrayList<>();
        List<String> incremental = new ArrayList<>();
        for (String symbol : symbols) {
            if (navHistoryRepo.countBySymbol(symbol) == 0) backfill.add(symbol);
            else incremental.add(symbol);
        }
        log.info("Fund NAV history sync: {} backfill, {} incremental", backfill.size(), incremental.size());

        int saved = 0;
        saved += syncNavHistoryBatches(backfill, BACKFILL_START_DATE);
        saved += syncNavHistoryBatches(incremental, LocalDate.now().minusMonths(3).toString());
        log.info("Fund NAV history sync complete: {} new entries", saved);
    }

    private int syncNavHistoryBatches(List<String> symbols, String startDate) {
        int saved = 0;
        for (List<String> batch : batches(symbols)) {
            try {
                List<BrapiFundNavHistoryResponse.NavEntry> entries =
                        brapiClient.fetchFundNavHistory(String.join(",", batch), startDate, null);
                Map<String, List<BrapiFundNavHistoryResponse.NavEntry>> bySymbol = entries.stream()
                        .collect(Collectors.groupingBy(BrapiFundNavHistoryResponse.NavEntry::symbol));
                for (var e : bySymbol.entrySet()) {
                    saved += saveNavHistory(e.getKey(), e.getValue());
                }
            } catch (Exception e) {
                log.warn("Failed to sync fund NAV history batch [{}...]: {}", batch.get(0), e.getMessage());
            }
        }
        return saved;
    }

    private int saveNavHistory(String symbol, List<BrapiFundNavHistoryResponse.NavEntry> entries) {
        Set<String> existingDates = navHistoryRepo.findBySymbolOrderByReferenceDateAsc(symbol)
                .stream().map(FundNavHistoryJpaEntity::getReferenceDate)
                .collect(Collectors.toCollection(java.util.HashSet::new));

        Instant now = Instant.now();
        List<FundNavHistoryJpaEntity> toSave = new ArrayList<>();
        for (var e : entries) {
            String refDate = normalizeDate(e.date());
            if (refDate == null || !existingDates.add(refDate)) continue;

            var entity = new FundNavHistoryJpaEntity();
            entity.setSymbol(symbol);
            entity.setReferenceDate(refDate);
            entity.setNavPerShare(e.navPerShare());
            entity.setEquity(e.equity());
            entity.setTotalAssets(e.totalAssets());
            if (e.totalInvestors() != null) entity.setTotalInvestors(e.totalInvestors().longValue());
            entity.setDailyApplications(e.dailyApplications());
            entity.setDailyRedemptions(e.dailyRedemptions());
            entity.setMonthlyReturn(e.monthlyReturn());
            entity.setSyncedAt(now);
            toSave.add(entity);
        }
        navHistoryRepo.saveAll(toSave);
        return toSave.size();
    }

    // ── Dividend events ──────────────────────────────────────────────────────

    private void syncDividends(List<String> symbols) {
        List<String> backfill = new ArrayList<>();
        List<String> incremental = new ArrayList<>();
        for (String symbol : symbols) {
            if (dividendRepo.countBySymbol(symbol) == 0) backfill.add(symbol);
            else incremental.add(symbol);
        }
        log.info("Fund dividends sync: {} backfill, {} incremental", backfill.size(), incremental.size());

        int saved = 0;
        saved += syncDividendBatches(backfill, null); // no startDate = full history
        saved += syncDividendBatches(incremental, LocalDate.now().minusMonths(3).toString());
        log.info("Fund dividends sync complete: {} new events", saved);
    }

    private int syncDividendBatches(List<String> symbols, String startDate) {
        int saved = 0;
        for (List<String> batch : batches(symbols)) {
            try {
                List<BrapiFundDividendsResponse.FundDividend> dividends =
                        brapiClient.fetchFundDividends(String.join(",", batch), startDate);
                Map<String, List<BrapiFundDividendsResponse.FundDividend>> bySymbol = dividends.stream()
                        .collect(Collectors.groupingBy(BrapiFundDividendsResponse.FundDividend::symbol));
                for (var e : bySymbol.entrySet()) {
                    saved += saveDividends(e.getKey(), e.getValue());
                }
            } catch (Exception e) {
                log.warn("Failed to sync fund dividends batch [{}...]: {}", batch.get(0), e.getMessage());
            }
        }
        return saved;
    }

    private int saveDividends(String symbol, List<BrapiFundDividendsResponse.FundDividend> dividends) {
        Set<String> existingKeys = dividendRepo.findBySymbolOrderByPaymentDateDesc(symbol)
                .stream().map(d -> d.getPaymentDate() + "|" + d.getRate())
                .collect(Collectors.toCollection(java.util.HashSet::new));

        Instant now = Instant.now();
        List<FundDividendEventJpaEntity> toSave = new ArrayList<>();
        for (var d : dividends) {
            String paymentDate = normalizeDate(d.paymentDate());
            if (!existingKeys.add(paymentDate + "|" + d.rate())) continue;

            var entity = new FundDividendEventJpaEntity();
            entity.setSymbol(symbol);
            entity.setDeclaredDate(normalizeDate(d.declaredDate()));
            entity.setLastDatePrior(normalizeDate(d.lastDatePrior()));
            entity.setPaymentDate(paymentDate);
            entity.setRate(d.rate());
            entity.setLabel(d.label());
            entity.setIsinCode(d.isinCode());
            entity.setSyncedAt(now);
            toSave.add(entity);
        }
        dividendRepo.saveAll(toSave);
        return toSave.size();
    }

    /**
     * brapi leaves dividendYield12m/1m empty for funds, so both are computed here
     * from the stored dividend events over the current market price.
     */
    private void refreshComputedYields(List<String> symbols) {
        LocalDate now = LocalDate.now();
        String cutoff12m = now.minusMonths(12).toString();
        String cutoff1m = now.minusMonths(1).toString();

        for (String symbol : symbols) {
            FundIndicatorJpaEntity entity = fundRepo.findById(symbol).orElse(null);
            if (entity == null || entity.getPrice() == null || entity.getPrice() <= 0) continue;

            double sum12m = 0, sum1m = 0;
            boolean any = false;
            for (FundDividendEventJpaEntity d : dividendRepo.findBySymbolOrderByPaymentDateDesc(symbol)) {
                if (d.getRate() == null || d.getPaymentDate() == null) continue;
                if (d.getPaymentDate().compareTo(cutoff12m) < 0) break; // list is desc-ordered
                any = true;
                sum12m += d.getRate();
                if (d.getPaymentDate().compareTo(cutoff1m) >= 0) sum1m += d.getRate();
            }
            if (!any) continue;
            entity.setDividendYield12m(round2(sum12m / entity.getPrice() * 100.0));
            entity.setDividendYield1m(round2(sum1m / entity.getPrice() * 100.0));
            fundRepo.save(entity);
        }
    }

    // ── Raw documents (profile / portfolio / reports) ────────────────────────

    private void syncDocuments(List<String> allSymbols, Map<String, List<String>> symbolsByType) {
        int saved = 0;
        saved += syncDocumentType("/api/v2/funds/profile", "profile", allSymbols);
        saved += syncDocumentType("/api/v2/funds/portfolio", "portfolio", allSymbols);

        List<String> fiagros = symbolsByType.getOrDefault("fiagro", List.of());
        saved += syncDocumentType("/api/v2/funds/fiagro/reports", "fiagro_report", fiagros);
        saved += syncDocumentType("/api/v2/funds/fiagro/portfolio", "fiagro_portfolio", fiagros);

        List<String> fidcs = symbolsByType.getOrDefault("fidc", List.of());
        saved += syncDocumentType("/api/v2/funds/fidc/reports", "fidc_report", fidcs);
        saved += syncDocumentType("/api/v2/funds/fidc/portfolio", "fidc_portfolio", fidcs);

        List<String> fips = symbolsByType.getOrDefault("fip", List.of());
        saved += syncDocumentType("/api/v2/funds/fip/reports", "fip_report", fips);

        log.info("Fund documents sync complete: {} new/updated documents", saved);
    }

    private int syncDocumentType(String path, String docType, List<String> symbols) {
        int saved = 0;
        for (List<String> batch : batches(symbols)) {
            try {
                List<Map<String, Object>> items = brapiClient.fetchFundDocuments(path, String.join(",", batch));
                for (Map<String, Object> item : items) {
                    if (saveDocument(docType, item)) saved++;
                }
            } catch (Exception e) {
                log.warn("Failed to sync fund documents {} batch [{}...]: {}", docType, batch.get(0), e.getMessage());
            }
        }
        return saved;
    }

    private boolean saveDocument(String docType, Map<String, Object> item) {
        String symbol = item.get("symbol") instanceof String s ? s : null;
        if (symbol == null) return false;
        String referenceDate = item.get("referenceDate") instanceof String d
                ? normalizeDate(d) : "latest";

        try {
            FundDocumentJpaEntity entity = documentRepo
                    .findBySymbolAndDocTypeAndReferenceDate(symbol, docType, referenceDate)
                    .orElseGet(() -> {
                        var e = new FundDocumentJpaEntity();
                        e.setSymbol(symbol);
                        e.setDocType(docType);
                        e.setReferenceDate(referenceDate);
                        return e;
                    });
            entity.setRawJson(JSON.writeValueAsString(item));
            entity.setSyncedAt(Instant.now());
            documentRepo.save(entity);
            return true;
        } catch (Exception e) {
            log.warn("Failed to save fund document {}/{}: {}", symbol, docType, e.getMessage());
            return false;
        }
    }

    // ── Ticker daily change ──────────────────────────────────────────────────

    /**
     * Fund quotes carry no daily change, so the hero variation is derived from the
     * last two navPerShare history entries (patrimonial daily change).
     */
    private void updateTickerDailyChanges(List<String> symbols) {
        for (String symbol : symbols) {
            List<FundNavHistoryJpaEntity> last2 =
                    navHistoryRepo.findTop2BySymbolOrderByReferenceDateDesc(symbol);
            if (last2.size() < 2) continue;
            Double current = last2.get(0).getNavPerShare();
            Double previous = last2.get(1).getNavPerShare();
            if (current == null || previous == null || previous <= 0) continue;

            tickerRepo.findBySymbol(symbol).ifPresent(ticker -> {
                ticker.setChangePercent(BigDecimal.valueOf((current - previous) / previous * 100.0));
                tickerRepo.save(ticker);
            });
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static List<List<String>> batches(List<String> symbols) {
        List<List<String>> result = new ArrayList<>();
        for (int i = 0; i < symbols.size(); i += BATCH_SIZE) {
            result.add(symbols.subList(i, Math.min(i + BATCH_SIZE, symbols.size())));
        }
        return result;
    }

    /** brapi fund dates come as ISO timestamps ("2026-06-18T00:00:00.000Z") — keep only the date. */
    private static String normalizeDate(String value) {
        if (value == null) return null;
        return value.length() > 10 ? value.substring(0, 10) : value;
    }

    private static Double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
