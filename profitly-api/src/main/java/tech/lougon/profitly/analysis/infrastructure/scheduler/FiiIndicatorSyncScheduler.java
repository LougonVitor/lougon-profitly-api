package tech.lougon.profitly.analysis.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.application.service.AnalysisService;
import tech.lougon.profitly.analysis.infrastructure.client.BrapiAnalysisClient;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiDividendsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiIndicatorsHistoryResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiListResponse;
import tech.lougon.profitly.analysis.infrastructure.persistence.DividendEventJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiIndicatorHistoryJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiIndicatorJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaDividendEventRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiIndicatorHistoryRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiIndicatorRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.JpaTickerRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.TickerJpaEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FiiIndicatorSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(FiiIndicatorSyncScheduler.class);

    private final JpaTickerRepository tickerRepo;
    private final JpaFiiIndicatorRepository indicatorRepo;
    private final JpaFiiIndicatorHistoryRepository historyRepo;
    private final JpaDividendEventRepository dividendRepo;
    private final BrapiAnalysisClient brapiClient;
    private final AnalysisService analysisService;

    public FiiIndicatorSyncScheduler(JpaTickerRepository tickerRepo,
                                     JpaFiiIndicatorRepository indicatorRepo,
                                     JpaFiiIndicatorHistoryRepository historyRepo,
                                     JpaDividendEventRepository dividendRepo,
                                     BrapiAnalysisClient brapiClient,
                                     AnalysisService analysisService) {
        this.tickerRepo = tickerRepo;
        this.indicatorRepo = indicatorRepo;
        this.historyRepo = historyRepo;
        this.dividendRepo = dividendRepo;
        this.brapiClient = brapiClient;
        this.analysisService = analysisService;
    }

    /** Runs once after Spring context is fully ready (non-blocking). Always syncs to ensure data is fresh. */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void syncOnStartup() {
        log.info("Running FII indicator startup sync");
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

        Set<String> synced = new java.util.LinkedHashSet<>();
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

        log.info("FII indicators synced: {}/{} — syncing history, dividends, prices", synced.size(), allFiis.size());

        for (String symbol : synced) {
            try {
                syncHistory(symbol);
            } catch (Exception e) {
                log.warn("FII indicator history sync failed for {}: {}", symbol, e.getMessage());
            }
            try {
                syncDividends(symbol);
            } catch (Exception e) {
                log.warn("FII dividend sync failed for {}: {}", symbol, e.getMessage());
            }
            try {
                analysisService.syncPriceHistory(symbol);
            } catch (Exception e) {
                log.warn("FII price history sync failed for {}: {}", symbol, e.getMessage());
            }
        }

        log.info("FII sync complete: {}/{} tickers synced", synced.size(), allFiis.size());
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

    private void syncHistory(String symbol) {
        boolean firstSync = historyRepo.countBySymbol(symbol) == 0;
        String startDate = firstSync ? "2016-01-01" : LocalDate.now().minusMonths(3).toString();

        List<BrapiFiiIndicatorsHistoryResponse.FiiHistoryEntry> entries =
                brapiClient.fetchFiiIndicatorsHistory(symbol, startDate, null);
        if (entries.isEmpty()) return;

        Set<String> existingDates = historyRepo.findBySymbolOrderByReferenceDateAsc(symbol)
                .stream().map(FiiIndicatorHistoryJpaEntity::getReferenceDate)
                .collect(Collectors.toSet());

        Instant now = Instant.now();
        for (var e : entries) {
            if (e.referenceDate() == null) continue;
            String refDate = e.referenceDate().length() > 10
                    ? e.referenceDate().substring(0, 10) : e.referenceDate();
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
        }
    }

    void syncDividends(String symbol) {
        List<BrapiFiiDividendsResponse.FiiDividend> dividends = brapiClient.fetchFiiDividends(symbol);
        if (dividends.isEmpty()) return;

        // deleteAll(entities) works without @Modifying transaction — avoids self-invocation proxy issue
        dividendRepo.deleteAll(dividendRepo.findBySymbolOrderByLastDatePriorDesc(symbol));

        for (var d : dividends) {
            var entity = new DividendEventJpaEntity();
            entity.setSymbol(symbol);
            entity.setLabel(d.label());
            entity.setRate(d.rate());
            entity.setPaymentDate(d.paymentDate());
            entity.setLastDatePrior(d.lastDatePrior());
            entity.setApprovedOn(d.approvedOn());
            entity.setRelatedTo(d.relatedTo());
            entity.setRemarks(d.remarks());
            dividendRepo.save(entity);
        }
    }

}
