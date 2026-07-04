package tech.lougon.profitly.analysis.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.infrastructure.client.BrapiAnalysisClient;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiIndicatorsHistoryResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiIndicatorsResponse;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiIndicatorHistoryJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.FiiIndicatorJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiIndicatorHistoryRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaFiiIndicatorRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.JpaTickerRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FiiIndicatorSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(FiiIndicatorSyncScheduler.class);
    private static final int BATCH_SIZE = 20;

    private final JpaTickerRepository tickerRepo;
    private final JpaFiiIndicatorRepository indicatorRepo;
    private final JpaFiiIndicatorHistoryRepository historyRepo;
    private final BrapiAnalysisClient brapiClient;

    public FiiIndicatorSyncScheduler(JpaTickerRepository tickerRepo,
                                     JpaFiiIndicatorRepository indicatorRepo,
                                     JpaFiiIndicatorHistoryRepository historyRepo,
                                     BrapiAnalysisClient brapiClient) {
        this.tickerRepo = tickerRepo;
        this.indicatorRepo = indicatorRepo;
        this.historyRepo = historyRepo;
        this.brapiClient = brapiClient;
    }

    /** Runs once after Spring context is fully ready (non-blocking). */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void syncOnStartup() {
        if (indicatorRepo.count() == 0) {
            log.info("fii_indicators table is empty — running initial FII indicator sync");
            syncAll();
        }
    }

    /** Nightly sync at 19:30, after main ticker sync (19:00). */
    @Scheduled(cron = "0 30 19 * * *", zone = "America/Sao_Paulo")
    public void syncAll() {
        List<String> fiiSymbols = tickerRepo.findAll().stream()
                .filter(t -> "FII".equalsIgnoreCase(t.getAssetType())
                        || "FII".equalsIgnoreCase(t.getSubType()))
                .map(t -> t.getSymbol())
                .toList();

        log.info("Syncing FII indicators for {} tickers in batches of {}", fiiSymbols.size(), BATCH_SIZE);

        // Step 1: sync current indicators in batches
        Set<String> synced = syncCurrentBatched(fiiSymbols);

        // Step 2: sync history only for symbols that returned data
        log.info("Syncing FII indicator history for {} tickers that have indicator data", synced.size());
        for (String symbol : synced) {
            try {
                syncHistory(symbol);
            } catch (Exception e) {
                log.warn("FII history sync failed for {}: {}", symbol, e.getMessage());
            }
        }

        log.info("FII indicator sync complete: {}/{} tickers had indicator data", synced.size(), fiiSymbols.size());
    }

    /** Fetches current indicators in batches of 20. Returns symbols that had data. */
    private Set<String> syncCurrentBatched(List<String> symbols) {
        Set<String> synced = new java.util.LinkedHashSet<>();
        List<List<String>> batches = partition(symbols, BATCH_SIZE);

        for (List<String> batch : batches) {
            try {
                String joined = String.join(",", batch);
                List<BrapiFiiIndicatorsResponse.FiiIndicatorWithInfo> results =
                        brapiClient.fetchFiiIndicatorsBatch(joined);

                for (BrapiFiiIndicatorsResponse.FiiIndicatorWithInfo info : results) {
                    if (info.symbol() == null || info.data() == null) continue;
                    try {
                        saveCurrent(info);
                        synced.add(info.symbol());
                    } catch (Exception e) {
                        log.warn("Failed to save FII indicator for {}: {}", info.symbol(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("FII batch sync failed for batch {}: {}", batch, e.getMessage());
            }
        }
        return synced;
    }

    private void saveCurrent(BrapiFiiIndicatorsResponse.FiiIndicatorWithInfo info) {
        String symbol = info.symbol();
        BrapiFiiIndicatorsResponse.FiiIndicator d = info.data();

        FiiIndicatorJpaEntity entity = indicatorRepo.findById(symbol)
                .orElseGet(() -> { var e = new FiiIndicatorJpaEntity(); e.setSymbol(symbol); return e; });

        entity.setAsOfDate(d.asOfDate());
        entity.setPrice(d.price());
        entity.setNavPerShare(d.navPerShare());
        entity.setPriceToNav(d.priceToNav());
        entity.setDividendYield12m(d.dividendYield12m());
        entity.setDividendYield1m(d.dividendYield1m());
        entity.setMonthlyReturn(d.monthlyReturn());
        entity.setTotalInvestors(d.totalInvestors());
        entity.setSharesOutstanding(d.sharesOutstanding());
        entity.setEquity(d.equity());
        entity.setTotalAssets(d.totalAssets());
        entity.setSegmentType(d.segmentType());
        if (info.administrator() != null) {
            entity.setAdminName(info.administrator().name());
            entity.setAdminCnpj(info.administrator().cnpj());
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

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}
