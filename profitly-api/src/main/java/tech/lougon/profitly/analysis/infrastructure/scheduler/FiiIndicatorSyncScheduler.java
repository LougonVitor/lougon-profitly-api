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
import tech.lougon.profitly.analysis.domain.model.PricePoint;
import tech.lougon.profitly.analysis.domain.repository.PriceHistoryRepository;
import tech.lougon.profitly.analysis.infrastructure.client.BrapiAnalysisClient;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiDividendsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiHistoricalResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiIndicatorsHistoryResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiListResponse;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiDividendEventJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiDocumentJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiIndicatorHistoryJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiIndicatorJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiDividendEventRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiDocumentRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiIndicatorHistoryRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiIndicatorRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.JpaTickerRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.TickerJpaEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Full sync pipeline for listed FIIs from the /api/v2/fii/* endpoints: catalog +
 * indicators + monthly indicator history + dividend events + market price history
 * + raw property/portfolio documents (vacancy, allocations).
 */
@Component
public class FiiIndicatorSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(FiiIndicatorSyncScheduler.class);

    /** brapi caps the FII document endpoints at 20 symbols per call. */
    private static final int BATCH_SIZE = 20;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final JpaTickerRepository tickerRepo;
    private final JpaFiiIndicatorRepository indicatorRepo;
    private final JpaFiiIndicatorHistoryRepository historyRepo;
    private final JpaFiiDividendEventRepository dividendRepo;
    private final JpaFiiDocumentRepository documentRepo;
    private final PriceHistoryRepository priceHistoryRepository;
    private final BrapiAnalysisClient brapiClient;

    public FiiIndicatorSyncScheduler(JpaTickerRepository tickerRepo,
                                     JpaFiiIndicatorRepository indicatorRepo,
                                     JpaFiiIndicatorHistoryRepository historyRepo,
                                     JpaFiiDividendEventRepository dividendRepo,
                                     JpaFiiDocumentRepository documentRepo,
                                     PriceHistoryRepository priceHistoryRepository,
                                     BrapiAnalysisClient brapiClient) {
        this.tickerRepo = tickerRepo;
        this.indicatorRepo = indicatorRepo;
        this.historyRepo = historyRepo;
        this.dividendRepo = dividendRepo;
        this.documentRepo = documentRepo;
        this.priceHistoryRepository = priceHistoryRepository;
        this.brapiClient = brapiClient;
    }

    @Value("${profitly.sync.on-startup:false}")
    private boolean syncOnStartup;

    // Dev-only flag while the FII screen is under construction — turn off when done
    @Value("${profitly.sync.fii-on-startup:false}")
    private boolean fiiSyncOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void syncOnStartup() {
        if (!syncOnStartup && !fiiSyncOnStartup) return;
        log.info("Running FII startup sync");
        syncAll();
    }

    /** Nightly sync at 19:30, after main ticker sync (19:00). */
    @Scheduled(cron = "0 30 19 * * *", zone = "America/Sao_Paulo")
    public void syncAll() {
        // Calling without ?symbols returns ALL FIIs that brapi has indexed
        List<BrapiFiiListResponse.FiiListItem> allFiis = brapiClient.fetchFiiList();
        if (allFiis.isEmpty()) {
            log.warn("FII list returned 0 results — skipping FII sync");
            return;
        }

        log.info("Syncing {} FIIs from brapi list", allFiis.size());

        List<String> synced = new ArrayList<>();
        for (BrapiFiiListResponse.FiiListItem item : allFiis) {
            if (item.symbol() == null) continue;
            try {
                saveCurrent(item);
                upsertTicker(item);
                synced.add(item.symbol());
            } catch (Exception e) {
                log.warn("Failed to save FII indicator for {}: {}", item.symbol(), e.getMessage());
            }
        }
        log.info("FII sync complete: {}/{} tickers synced", synced.size(), allFiis.size());

        syncIndicatorHistories(synced);
        syncDividends(synced);
        syncDocuments(synced);
        syncPriceHistories(synced);
        updateTickerDailyChanges(synced);
    }

    private void upsertTicker(BrapiFiiListResponse.FiiListItem item) {
        TickerJpaEntity ticker = tickerRepo.findBySymbol(item.symbol())
                .orElseGet(TickerJpaEntity::new);

        String name = item.name() != null ? item.name() : item.symbol();
        ticker.setSymbol(item.symbol());
        ticker.setName(name);
        ticker.setLongName(name);
        ticker.setAssetType("FII");
        ticker.setSubType(item.segmentType());
        ticker.setIsActive(true);
        if (item.price() != null) {
            ticker.setLastPrice(BigDecimal.valueOf(item.price()));
        }
        ticker.setSyncedAt(Instant.now());
        tickerRepo.save(ticker);
    }

    private void saveCurrent(BrapiFiiListResponse.FiiListItem item) {
        FiiIndicatorJpaEntity entity = indicatorRepo.findById(item.symbol())
                .orElseGet(() -> { var e = new FiiIndicatorJpaEntity(); e.setSymbol(item.symbol()); return e; });

        entity.setName(item.name());
        entity.setCnpj(item.cnpj());
        entity.setMandate(item.mandate());
        entity.setSegmentoAtuacao(item.segmentoAtuacao());
        entity.setTipoGestao(item.tipoGestao());
        entity.setPrice(item.price());
        entity.setNavPerShare(item.navPerShare());
        entity.setPriceToNav(item.priceToNav());
        entity.setDividendYield12m(item.dividendYield12m());
        entity.setSegmentType(item.segmentType());
        entity.setAdminName(item.administratorName());
        entity.setAdminCnpj(item.administratorCnpj());
        if (item.totalInvestors() != null) {
            entity.setTotalInvestors(item.totalInvestors().longValue());
        }
        entity.setSyncedAt(Instant.now());
        indicatorRepo.save(entity);
    }

    // ── Monthly indicator history (P/VP, DY, equity, investors over time) ────

    private void syncIndicatorHistories(List<String> symbols) {
        int saved = 0;
        for (String symbol : symbols) {
            try {
                saved += syncIndicatorHistory(symbol);
            } catch (Exception e) {
                log.warn("Failed to sync FII indicator history for {}: {}", symbol, e.getMessage());
            }
        }
        log.info("FII indicator history sync complete: {} new entries", saved);
    }

    private int syncIndicatorHistory(String symbol) {
        boolean firstSync = historyRepo.countBySymbol(symbol) == 0;
        String startDate = firstSync ? "2016-01-01" : LocalDate.now().minusMonths(3).toString();

        List<BrapiFiiIndicatorsHistoryResponse.FiiHistoryEntry> entries =
                brapiClient.fetchFiiIndicatorsHistory(symbol, startDate, null);
        if (entries.isEmpty()) return 0;

        Set<String> existingDates = historyRepo.findBySymbolOrderByReferenceDateAsc(symbol)
                .stream().map(FiiIndicatorHistoryJpaEntity::getReferenceDate)
                .collect(Collectors.toSet());

        int saved = 0;
        Instant now = Instant.now();
        for (var e : entries) {
            if (e.referenceDate() == null) continue;
            String refDate = normalizeDate(e.referenceDate());
            if (existingDates.contains(refDate)) continue;

            var entity = new FiiIndicatorHistoryJpaEntity();
            entity.setSymbol(symbol);
            entity.setReferenceDate(refDate);
            entity.setPrice(e.price());
            entity.setNavPerShare(e.navPerShare());
            entity.setPriceToNav(e.priceToNav());
            entity.setDividendYield12m(e.dividendYield12m());
            entity.setDividendYield1m(e.dividendYield1m());
            entity.setMonthlyReturn(e.monthlyReturn());
            entity.setTotalInvestors(e.totalInvestors());
            entity.setSharesOutstanding(e.sharesOutstanding());
            entity.setEquity(e.equity());
            entity.setTotalAssets(e.totalAssets());
            entity.setSegmentType(e.segmentType());
            entity.setSyncedAt(now);
            historyRepo.save(entity);
            existingDates.add(refDate);
            saved++;
        }
        return saved;
    }

    // ── Dividends ─────────────────────────────────────────────────────────────

    private void syncDividends(List<String> symbols) {
        int saved = 0;
        for (String symbol : symbols) {
            try {
                saved += syncDividendsFor(symbol);
            } catch (Exception e) {
                log.warn("Failed to sync FII dividends for {}: {}", symbol, e.getMessage());
            }
        }
        log.info("FII dividends sync complete: {} events saved", saved);
    }

    private int syncDividendsFor(String symbol) {
        List<BrapiFiiDividendsResponse.FiiDividend> dividends = brapiClient.fetchFiiDividends(symbol);
        if (dividends.isEmpty()) return 0;

        // deleteAll(entities) works without @Modifying transaction — avoids self-invocation proxy issue
        dividendRepo.deleteAll(dividendRepo.findBySymbolOrderByPaymentDateDesc(symbol));

        int saved = 0;
        Instant now = Instant.now();
        for (var d : dividends) {
            if (d.rate() == null) continue;
            var entity = new FiiDividendEventJpaEntity();
            entity.setSymbol(symbol);
            entity.setLabel(d.label());
            entity.setRate(d.rate());
            entity.setPaymentDate(d.paymentDate());
            entity.setLastDatePrior(d.lastDatePrior());
            entity.setApprovedOn(d.approvedOn());
            entity.setRelatedTo(d.relatedTo());
            entity.setIsinCode(d.isinCode());
            entity.setSyncedAt(now);
            dividendRepo.save(entity);
            saved++;
        }
        return saved;
    }

    // ── Raw documents (properties / portfolio, current + quarterly history) ──

    private void syncDocuments(List<String> symbols) {
        int saved = 0;
        saved += syncDocumentType("/api/v2/fii/properties", "properties", symbols);
        saved += syncDocumentType("/api/v2/fii/portfolio", "portfolio", symbols);
        saved += syncDocumentType("/api/v2/fii/properties/history", "properties_history", symbols);
        saved += syncDocumentType("/api/v2/fii/portfolio/history", "portfolio_history", symbols);
        log.info("FII documents sync complete: {} new/updated documents", saved);
    }

    private int syncDocumentType(String path, String docType, List<String> symbols) {
        int saved = 0;
        for (List<String> batch : batches(symbols)) {
            try {
                List<Map<String, Object>> items = brapiClient.fetchFiiDocuments(path, String.join(",", batch));
                for (Map<String, Object> item : items) {
                    if (saveDocument(docType, item)) saved++;
                }
            } catch (Exception e) {
                log.warn("Failed to sync FII documents {} batch [{}...]: {}", docType, batch.get(0), e.getMessage());
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
            FiiDocumentJpaEntity entity = documentRepo
                    .findBySymbolAndDocTypeAndReferenceDate(symbol, docType, referenceDate)
                    .orElseGet(() -> {
                        var e = new FiiDocumentJpaEntity();
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
            log.warn("Failed to save FII document {}/{}: {}", symbol, docType, e.getMessage());
            return false;
        }
    }

    // ── Market price history ─────────────────────────────────────────────────

    /**
     * FII market price bars come from the FII-specific /fii/historical endpoint
     * (distinct response shape from the generic /stocks/historical used by funds).
     * They go into price_points — the same table the generic
     * /api/analysis/{symbol}/history chart reads from — with no FII-specific code.
     */
    private void syncPriceHistories(List<String> symbols) {
        int backfilled = 0, incremented = 0, barsSaved = 0;
        for (String symbol : symbols) {
            boolean backfill = priceHistoryRepository.findLatestDateBySymbol(symbol).isEmpty();
            try {
                List<BrapiFiiHistoricalResponse.PriceBar> bars =
                        brapiClient.fetchFiiHistory(symbol, backfill ? "max" : "3mo");
                barsSaved += storePriceBars(symbol, bars);
                if (backfill) backfilled++; else incremented++;
            } catch (Exception e) {
                log.warn("Failed to sync price history for FII {}: {}", symbol, e.getMessage());
            }
        }
        log.info("FII price history sync complete: {} backfill, {} incremental, {} bars saved",
                backfilled, incremented, barsSaved);
    }

    /** Inserts only bars newer than the latest stored date (price_points has a unique symbol+date). */
    private int storePriceBars(String symbol, List<BrapiFiiHistoricalResponse.PriceBar> bars) {
        if (bars == null || bars.isEmpty()) return 0;

        LocalDate latest = priceHistoryRepository.findLatestDateBySymbol(symbol).orElse(null);
        List<PricePoint> points = bars.stream()
                .filter(b -> b.date() != null && b.close() != null)
                .map(b -> new PricePoint(
                        symbol,
                        Instant.ofEpochSecond(b.date()).atZone(ZoneOffset.UTC).toLocalDate(),
                        b.open() != null ? BigDecimal.valueOf(b.open()) : null,
                        b.high() != null ? BigDecimal.valueOf(b.high()) : null,
                        b.low() != null ? BigDecimal.valueOf(b.low()) : null,
                        BigDecimal.valueOf(b.close()),
                        b.adjustedClose() != null ? BigDecimal.valueOf(b.adjustedClose()) : null,
                        b.volume()
                ))
                .filter(p -> latest == null || p.date().isAfter(latest))
                .toList();

        if (!points.isEmpty()) {
            priceHistoryRepository.saveAll(points);
        }
        return points.size();
    }

    // ── Ticker daily change ──────────────────────────────────────────────────

    /** Derives the hero daily variation from the last two market price bars, falling back to indicator history. */
    private void updateTickerDailyChanges(List<String> symbols) {
        LocalDate today = LocalDate.now();
        for (String symbol : symbols) {
            Double current = null, previous = null;

            List<PricePoint> recent = priceHistoryRepository
                    .findBySymbolAndDateBetween(symbol, today.minusDays(15), today);
            if (recent.size() >= 2) {
                PricePoint last = recent.get(recent.size() - 1);
                PricePoint prior = recent.get(recent.size() - 2);
                if (last.close() != null && prior.close() != null) {
                    current = last.close().doubleValue();
                    previous = prior.close().doubleValue();
                }
            }
            if (current == null) {
                List<FiiIndicatorHistoryJpaEntity> hist =
                        historyRepo.findBySymbolOrderByReferenceDateAsc(symbol);
                if (hist.size() < 2) continue;
                current = hist.get(hist.size() - 1).getPrice();
                previous = hist.get(hist.size() - 2).getPrice();
            }
            if (current == null || previous == null || previous <= 0) continue;

            double changePercent = (current - previous) / previous * 100.0;
            tickerRepo.findBySymbol(symbol).ifPresent(t -> {
                t.setChangePercent(BigDecimal.valueOf(changePercent));
                tickerRepo.save(t);
            });
        }
    }

    private static List<List<String>> batches(List<String> symbols) {
        List<List<String>> result = new ArrayList<>();
        for (int i = 0; i < symbols.size(); i += BATCH_SIZE) {
            result.add(symbols.subList(i, Math.min(i + BATCH_SIZE, symbols.size())));
        }
        return result;
    }

    /** brapi FII dates come as ISO timestamps or plain dates — keep only the date. */
    private static String normalizeDate(String value) {
        return value.length() > 10 ? value.substring(0, 10) : value;
    }
}
