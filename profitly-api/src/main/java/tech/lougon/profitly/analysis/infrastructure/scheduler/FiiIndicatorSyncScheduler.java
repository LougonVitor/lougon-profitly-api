package tech.lougon.profitly.analysis.infrastructure.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.domain.model.PricePoint;
import tech.lougon.profitly.analysis.domain.repository.PriceHistoryRepository;
import tech.lougon.profitly.analysis.infrastructure.client.BrapiAnalysisClient;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiDividendsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiDividendsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiHistoricalResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiIndicatorsHistoryResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiIndicatorsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiListResponse;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiDividendEventJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiDocumentJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiIndicatorHistoryJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiIndicatorJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiDividendEventRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiDocumentRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiIndicatorHistoryRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiIndicatorRepository;
import tech.lougon.profitly.ticker.infrastructure.client.BrapiClient;
import tech.lougon.profitly.ticker.infrastructure.client.dto.BrapiTickerResponse;
import tech.lougon.profitly.ticker.infrastructure.persistence.JpaTickerRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.TickerJpaEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Full sync pipeline for listed FIIs from the /api/v2/fii/* endpoints: catalog +
 * monthly indicators + monthly indicator history + dividend events + market price
 * history + raw property/portfolio documents (vacancy, allocations).
 *
 * Every brapi endpoint here accepts up to 20 symbols per call, so all phases batch
 * symbols in groups of 20 — one call per group, not one per FII.
 */
@Component
public class FiiIndicatorSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(FiiIndicatorSyncScheduler.class);

    /** brapi caps the FII endpoints at 20 symbols per call. */
    private static final int BATCH_SIZE = 20;
    private static final String HISTORY_BACKFILL_START = "2016-01-01";
    private static final String DIVIDEND_BACKFILL_START = "2016-01-01";
    private static final String PRICE_BACKFILL_START = "2015-01-01";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final JpaTickerRepository tickerRepo;
    private final JpaFiiIndicatorRepository indicatorRepo;
    private final JpaFiiIndicatorHistoryRepository historyRepo;
    private final JpaFiiDividendEventRepository dividendRepo;
    private final JpaFiiDocumentRepository documentRepo;
    private final PriceHistoryRepository priceHistoryRepository;
    private final BrapiAnalysisClient brapiClient;
    private final BrapiClient tickerClient;

    public FiiIndicatorSyncScheduler(JpaTickerRepository tickerRepo,
                                     JpaFiiIndicatorRepository indicatorRepo,
                                     JpaFiiIndicatorHistoryRepository historyRepo,
                                     JpaFiiDividendEventRepository dividendRepo,
                                     JpaFiiDocumentRepository documentRepo,
                                     PriceHistoryRepository priceHistoryRepository,
                                     BrapiAnalysisClient brapiClient,
                                     BrapiClient tickerClient) {
        this.tickerRepo = tickerRepo;
        this.indicatorRepo = indicatorRepo;
        this.historyRepo = historyRepo;
        this.dividendRepo = dividendRepo;
        this.documentRepo = documentRepo;
        this.priceHistoryRepository = priceHistoryRepository;
        this.brapiClient = brapiClient;
        this.tickerClient = tickerClient;
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

    /** Invoked by the unified 18h market synchronizer after the ticker catalog. */
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
        log.info("FII catalog synced: {}/{} tickers", synced.size(), allFiis.size());

        List<String> orphans = seedOrphanFiis(synced);

        // Vertical-only phases: these endpoints 404 for the orphans, so they get `synced` only
        syncIndicators(synced);
        syncIndicatorHistories(synced);
        syncDividends(synced);
        syncDocuments(synced);

        // /fii/historical DOES serve the orphans even though /fii/list omits them
        List<String> all = new ArrayList<>(synced);
        all.addAll(orphans);
        syncPriceHistories(all);
        syncLegacyDividends(orphans);
        updateTickerDailyChanges(all);
        log.info("FII sync complete");
    }

    // ── FIIs missing from the /fii/* vertical ────────────────────────────────

    /**
     * brapi keeps two disjoint FII sources: the /fii/* vertical (list, indicators,
     * dividends, documents) and the general /tickers catalog. A few dozen funds —
     * BTHF11, BTCI11, FIIB11, EURO11 — sit in the catalog but are absent from the
     * vertical, so the catalog loop above never seeds them into fii_indicators and
     * /api/fii/analysis/{symbol} 404s on a ticker the UI still routes to the FII page
     * (the front dispatches on subType=fii).
     *
     * Seeds those orphans from what the catalog does carry (name, price). The
     * vertical-only metrics (P/VP, DY, equity, vacancy, portfolio) stay null and the
     * screen drops those sections, but price history, returns and the chart render.
     */
    private List<String> seedOrphanFiis(List<String> covered) {
        List<BrapiTickerResponse.TickerItem> catalog;
        try {
            catalog = tickerClient.fetchTickersBySubType("fii");
        } catch (Exception e) {
            log.warn("Failed to fetch the FII ticker catalog — skipping orphan seed: {}", e.getMessage());
            return List.of();
        }

        Set<String> inVertical = new HashSet<>(covered);
        List<String> orphans = new ArrayList<>();
        for (BrapiTickerResponse.TickerItem item : catalog) {
            if (item.symbol() == null || inVertical.contains(item.symbol())) continue;
            try {
                seedOrphan(item);
                upsertOrphanTicker(item);
                orphans.add(item.symbol());
            } catch (Exception e) {
                log.warn("Failed to seed orphan FII {}: {}", item.symbol(), e.getMessage());
            }
        }
        log.info("Seeded {} FIIs present in the ticker catalog but absent from /fii/list", orphans.size());
        return orphans;
    }

    private void seedOrphan(BrapiTickerResponse.TickerItem item) {
        FiiIndicatorJpaEntity entity = indicatorRepo.findById(item.symbol())
                .orElseGet(() -> { var e = new FiiIndicatorJpaEntity(); e.setSymbol(item.symbol()); return e; });

        String name = item.longName() != null ? item.longName() : item.name();
        if (name != null) entity.setName(name);
        if (item.quote() != null && item.quote().lastPrice() != null) {
            entity.setPrice(item.quote().lastPrice().doubleValue());
        }
        entity.setSyncedAt(Instant.now());
        indicatorRepo.save(entity);
    }

    /**
     * The orphans need a tickers row of their own: the general catalog sync only walks
     * stock/unit/bdr/fidc/fip (see BrapiClient.GENERAL_SUB_TYPES) and upsertTicker above
     * only runs for /fii/list members, so nothing else writes them. Without this row the
     * fund is unreachable — search can't find it and the front resolves the FII page off
     * the ticker's assetType. Seeds assetType=FII like the vertical does; segmentType is
     * left alone because /fii/indicators 404s for these funds, so the segment is unknown.
     */
    private void upsertOrphanTicker(BrapiTickerResponse.TickerItem item) {
        TickerJpaEntity ticker = tickerRepo.findBySymbol(item.symbol())
                .orElseGet(TickerJpaEntity::new);

        ticker.setSymbol(item.symbol());
        ticker.setName(item.name() != null ? item.name() : item.symbol());
        ticker.setLongName(item.longName() != null ? item.longName() : ticker.getName());
        ticker.setAssetType("FII");
        ticker.setIsActive(item.isActive() == null || item.isActive());
        if (item.quote() != null && item.quote().lastPrice() != null) {
            ticker.setLastPrice(item.quote().lastPrice());
        }
        ticker.setSyncedAt(Instant.now());
        tickerRepo.save(ticker);
    }

    /**
     * /fii/dividends returns nothing for the orphans, but the legacy
     * /api/quote/{symbol}?dividends=true still carries payouts for some of them
     * (BTCI11, FIIB11, EURO11, SCPF11, HUCG11, BIME11 at the time of writing), which is
     * enough for DY and the Magic Number. One request per symbol — the legacy endpoint
     * takes a single symbol — but the orphan set is a few dozen, not the full 1000+.
     * Funds with no payouts anywhere in brapi (BTHF11) just stay empty and the DY
     * sections stay hidden.
     */
    private void syncLegacyDividends(List<String> symbols) {
        int saved = 0;
        int withData = 0;
        int restamped = 0;
        for (String symbol : symbols) {
            try {
                List<BrapiDividendsResponse.CashDividend> dividends = brapiClient.fetchLegacyDividends(symbol);
                if (dividends.isEmpty()) continue;
                withData++;

                Map<String, FiiDividendEventJpaEntity> existing = new HashMap<>();
                for (FiiDividendEventJpaEntity d : dividendRepo.findBySymbolOrderByPaymentDateDesc(symbol)) {
                    existing.put(dividendKey(d.getSymbol(), d.getPaymentDate(), d.getRate()), d);
                }
                Instant now = Instant.now();
                for (var d : dividends) {
                    String key = dividendKey(symbol, d.paymentDate(), d.rate());
                    FiiDividendEventJpaEntity stored = existing.get(key);
                    if (stored != null) {
                        // Rows written before `source` existed default to VERTICAL — restamp them
                        // here so the split-unsafe metrics get suppressed without a manual backfill
                        if (!"LEGACY".equals(stored.getSource())) {
                            stored.setSource("LEGACY");
                            dividendRepo.save(stored);
                            restamped++;
                        }
                        continue;
                    }

                    var entity = new FiiDividendEventJpaEntity();
                    entity.setSymbol(symbol);
                    entity.setLabel(d.label());
                    entity.setRate(d.rate());
                    entity.setPaymentDate(normalizeDate(d.paymentDate()));
                    entity.setLastDatePrior(normalizeDate(d.lastDatePrior()));
                    entity.setApprovedOn(normalizeDate(d.approvedOn()));
                    entity.setRelatedTo(d.relatedTo());
                    entity.setIsinCode(d.isinCode());
                    entity.setSource("LEGACY");
                    entity.setSyncedAt(now);
                    dividendRepo.save(entity);
                    existing.put(key, entity);
                    saved++;
                }
            } catch (Exception e) {
                log.warn("Failed legacy dividend sync for {}: {}", symbol, e.getMessage());
            }
        }
        log.info("Legacy FII dividends: {} new events, {} restamped, across {}/{} orphans",
                saved, restamped, withData, symbols.size());
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

    // ── Monthly indicators (enrich equity, totalAssets, DY 1m, asOfDate, ...) ──

    /**
     * /fii/list omits equity, totalAssets, sharesOutstanding, dividendYield1m,
     * monthlyReturn and asOfDate — those only come from /fii/indicators. Fetched in
     * batches of 20 and merged onto the catalog rows.
     */
    private void syncIndicators(List<String> symbols) {
        int updated = 0;
        for (List<String> batch : batches(symbols)) {
            List<BrapiFiiIndicatorsResponse.FiiIndicator> results =
                    brapiClient.fetchFiiIndicatorsBatch(String.join(",", batch));
            for (var ind : results) {
                FiiIndicatorJpaEntity entity = indicatorRepo.findById(ind.symbol()).orElse(null);
                if (entity == null) continue;
                entity.setAsOfDate(normalizeDate(ind.asOfDate()));
                if (ind.price() != null) entity.setPrice(ind.price());
                if (ind.navPerShare() != null) entity.setNavPerShare(ind.navPerShare());
                if (ind.priceToNav() != null) entity.setPriceToNav(ind.priceToNav());
                if (ind.dividendYield12m() != null) entity.setDividendYield12m(ind.dividendYield12m());
                if (ind.dividendYield1m() != null) entity.setDividendYield1m(ind.dividendYield1m());
                if (ind.monthlyReturn() != null) entity.setMonthlyReturn(ind.monthlyReturn());
                if (ind.equity() != null) entity.setEquity(ind.equity());
                if (ind.totalAssets() != null) entity.setTotalAssets(ind.totalAssets());
                if (ind.sharesOutstanding() != null) entity.setSharesOutstanding(ind.sharesOutstanding());
                if (ind.totalInvestors() != null) entity.setTotalInvestors(ind.totalInvestors());
                if (ind.segmentType() != null) entity.setSegmentType(ind.segmentType());
                if (ind.segmentoAtuacao() != null) entity.setSegmentoAtuacao(ind.segmentoAtuacao());
                if (ind.tipoGestao() != null) entity.setTipoGestao(ind.tipoGestao());
                if (ind.administratorName() != null) entity.setAdminName(ind.administratorName());
                if (ind.administratorCnpj() != null) entity.setAdminCnpj(ind.administratorCnpj());
                entity.setSyncedAt(Instant.now());
                indicatorRepo.save(entity);
                updated++;
            }
        }
        log.info("FII indicators enriched: {}/{}", updated, symbols.size());
    }

    // ── Monthly indicator history (P/VP, DY, equity, investors over time) ────

    private void syncIndicatorHistories(List<String> symbols) {
        List<String> backfill = new ArrayList<>();
        List<String> incremental = new ArrayList<>();
        for (String symbol : symbols) {
            if (historyRepo.countBySymbol(symbol) == 0) backfill.add(symbol);
            else incremental.add(symbol);
        }
        int saved = 0;
        saved += syncHistoryBatches(backfill, HISTORY_BACKFILL_START);
        saved += syncHistoryBatches(incremental, LocalDate.now().minusMonths(3).toString());
        log.info("FII indicator history sync complete: {} backfill, {} incremental, {} new entries",
                backfill.size(), incremental.size(), saved);
    }

    private int syncHistoryBatches(List<String> symbols, String startDate) {
        int saved = 0;
        for (List<String> batch : batches(symbols)) {
            try {
                List<BrapiFiiIndicatorsHistoryResponse.FiiHistoryEntry> entries =
                        brapiClient.fetchFiiIndicatorsHistoryBatch(String.join(",", batch), startDate);
                // existing (symbol, refDate) keys for this batch, to insert only new rows
                Set<String> existing = new HashSet<>();
                for (String symbol : batch) {
                    for (FiiIndicatorHistoryJpaEntity h : historyRepo.findBySymbolOrderByReferenceDateAsc(symbol)) {
                        existing.add(h.getSymbol() + "|" + h.getReferenceDate());
                    }
                }
                Instant now = Instant.now();
                for (var e : entries) {
                    String refDate = normalizeDate(e.referenceDate());
                    String key = e.symbol() + "|" + refDate;
                    if (existing.contains(key)) continue;

                    var entity = new FiiIndicatorHistoryJpaEntity();
                    entity.setSymbol(e.symbol());
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
                    existing.add(key);
                    saved++;
                }
            } catch (Exception e) {
                log.warn("Failed to sync FII history batch [{}...]: {}", batch.get(0), e.getMessage());
            }
        }
        return saved;
    }

    // ── Dividends ─────────────────────────────────────────────────────────────

    private void syncDividends(List<String> symbols) {
        List<String> backfill = new ArrayList<>();
        List<String> incremental = new ArrayList<>();
        for (String symbol : symbols) {
            if (dividendRepo.countBySymbol(symbol) == 0) backfill.add(symbol);
            else incremental.add(symbol);
        }
        int saved = 0;
        saved += syncDividendBatches(backfill, DIVIDEND_BACKFILL_START);
        saved += syncDividendBatches(incremental, LocalDate.now().minusMonths(3).toString());
        log.info("FII dividends sync complete: {} backfill, {} incremental, {} new events",
                backfill.size(), incremental.size(), saved);
    }

    private int syncDividendBatches(List<String> symbols, String startDate) {
        int saved = 0;
        for (List<String> batch : batches(symbols)) {
            try {
                List<BrapiFiiDividendsResponse.FiiDividend> dividends =
                        brapiClient.fetchFiiDividendsBatch(String.join(",", batch), startDate);
                // existing (symbol, paymentDate, rate) keys, to insert only new events
                Set<String> existing = new HashSet<>();
                for (String symbol : batch) {
                    for (FiiDividendEventJpaEntity d : dividendRepo.findBySymbolOrderByPaymentDateDesc(symbol)) {
                        existing.add(dividendKey(d.getSymbol(), d.getPaymentDate(), d.getRate()));
                    }
                }
                Instant now = Instant.now();
                for (var d : dividends) {
                    String key = dividendKey(d.symbol(), d.paymentDate(), d.rate());
                    if (existing.contains(key)) continue;

                    var entity = new FiiDividendEventJpaEntity();
                    entity.setSymbol(d.symbol());
                    entity.setLabel(d.label());
                    entity.setRate(d.rate());
                    entity.setPaymentDate(d.paymentDate());
                    entity.setLastDatePrior(d.lastDatePrior());
                    entity.setApprovedOn(d.approvedOn());
                    entity.setRelatedTo(d.relatedTo());
                    entity.setIsinCode(d.isinCode());
                    entity.setSource("VERTICAL");
                    entity.setSyncedAt(now);
                    dividendRepo.save(entity);
                    existing.add(key);
                    saved++;
                }
            } catch (Exception e) {
                log.warn("Failed to sync FII dividends batch [{}...]: {}", batch.get(0), e.getMessage());
            }
        }
        return saved;
    }

    /**
     * Dates are normalized into the key because the two dividend sources format them
     * differently — /fii/dividends returns "2026-05-29 00:00:00+00" while the legacy
     * fallback returns a plain date. A fund that gains vertical coverage after being
     * seeded by the legacy path would otherwise miss every existing row and re-insert
     * its whole payout history.
     */
    private static String dividendKey(String symbol, String paymentDate, Double rate) {
        return symbol + "|" + normalizeDate(paymentDate) + "|" + rate;
    }

    // ── Raw documents (properties / portfolio, current + quarterly history) ──

    private void syncDocuments(List<String> symbols) {
        int saved = 0;
        saved += syncReports(symbols);
        saved += syncDocumentType("/api/v2/fii/properties", "properties", symbols);
        saved += syncDocumentType("/api/v2/fii/portfolio", "portfolio", symbols);
        saved += syncDocumentType("/api/v2/fii/properties/history", "properties_history", symbols);
        saved += syncDocumentType("/api/v2/fii/portfolio/history", "portfolio_history", symbols);
        log.info("FII documents sync complete: {} new/updated documents", saved);
    }

    /**
     * Stores the last ~13 monthly reports per FII (one "report" doc per month) so the
     * analysis can average the noisy monthly management fee instead of trusting a single
     * month that may carry a one-off performance fee.
     */
    private int syncReports(List<String> symbols) {
        String startDate = LocalDate.now().minusMonths(13).toString();
        int saved = 0;
        for (List<String> batch : batches(symbols)) {
            try {
                List<Map<String, Object>> items = brapiClient.fetchFiiReports(String.join(",", batch), startDate);
                for (Map<String, Object> item : items) {
                    if (saveDocument("report", item)) saved++;
                }
            } catch (Exception e) {
                log.warn("Failed to sync FII reports batch [{}...]: {}", batch.get(0), e.getMessage());
            }
        }
        return saved;
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
     * That endpoint is bounded by startDate (NOT a range param), so backfill passes an
     * early date. Batched by 20 symbols per call. Bars go into price_points — the same
     * table the generic /api/analysis/{symbol}/history chart reads from.
     */
    private void syncPriceHistories(List<String> symbols) {
        List<String> backfill = new ArrayList<>();
        List<String> incremental = new ArrayList<>();
        for (String symbol : symbols) {
            if (priceHistoryRepository.findLatestDateBySymbol(symbol).isEmpty()) backfill.add(symbol);
            else incremental.add(symbol);
        }
        int barsSaved = 0;
        barsSaved += syncPriceBatches(backfill, PRICE_BACKFILL_START);
        barsSaved += syncPriceBatches(incremental, LocalDate.now().minusMonths(3).toString());
        log.info("FII price history sync complete: {} backfill, {} incremental, {} bars saved",
                backfill.size(), incremental.size(), barsSaved);
    }

    private int syncPriceBatches(List<String> symbols, String startDate) {
        int barsSaved = 0;
        for (List<String> batch : batches(symbols)) {
            try {
                List<BrapiFiiHistoricalResponse.FiiHistoricalResult> results =
                        brapiClient.fetchFiiHistoryBatch(String.join(",", batch), startDate);
                for (var r : results) {
                    barsSaved += storePriceBars(r.symbol(), r.historicalDataPrice());
                }
            } catch (Exception e) {
                log.warn("Failed to sync FII price batch [{}...]: {}", batch.get(0), e.getMessage());
            }
        }
        return barsSaved;
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
        if (value == null) return null;
        return value.length() > 10 ? value.substring(0, 10) : value;
    }
}
