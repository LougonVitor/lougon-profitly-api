package tech.lougon.profitly.analysis.infrastructure.scheduler;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.List;

@Component
public class FiiIndicatorSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(FiiIndicatorSyncScheduler.class);

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

    /** Runs once on startup (async so it doesn't block Spring context init). */
    @PostConstruct
    @Async
    public void syncOnStartup() {
        if (indicatorRepo.count() == 0) {
            log.info("fii_indicators table is empty — running initial sync on startup");
            syncAll();
        }
    }

    /** Nightly sync at 19:30, after the main ticker sync (19:00). */
    @Scheduled(cron = "0 30 19 * * *", zone = "America/Sao_Paulo")
    public void syncAll() {
        List<String> fiiSymbols = tickerRepo.findAll().stream()
                .filter(t -> "FII".equalsIgnoreCase(t.getAssetType())
                        || "FII".equalsIgnoreCase(t.getSubType()))
                .map(t -> t.getSymbol())
                .toList();

        log.info("Syncing FII indicators for {} tickers", fiiSymbols.size());
        int ok = 0;
        for (String symbol : fiiSymbols) {
            try {
                syncCurrent(symbol);
                syncHistory(symbol);
                ok++;
            } catch (Exception e) {
                log.warn("FII indicator sync failed for {}: {}", symbol, e.getMessage());
            }
        }
        log.info("FII indicator sync done: {}/{}", ok, fiiSymbols.size());
    }

    private void syncCurrent(String symbol) {
        var opt = brapiClient.fetchFiiIndicators(symbol);
        if (opt.isEmpty()) return;

        BrapiFiiIndicatorsResponse.FiiIndicatorWithInfo info = opt.get();
        BrapiFiiIndicatorsResponse.FiiIndicator d = info.data();
        if (d == null) return;

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
        // First sync: fetch full history from 2016. Subsequent: last 3 months.
        boolean firstSync = historyRepo.countBySymbol(symbol) == 0;
        String startDate = firstSync ? "2016-01-01" : LocalDate.now().minusMonths(3).toString();

        List<BrapiFiiIndicatorsHistoryResponse.FiiHistoryEntry> entries =
                brapiClient.fetchFiiIndicatorsHistory(symbol, startDate, null);
        if (entries.isEmpty()) return;

        // Collect existing dates to avoid duplicate inserts
        java.util.Set<String> existingDates = historyRepo.findBySymbolOrderByReferenceDateAsc(symbol)
                .stream().map(FiiIndicatorHistoryJpaEntity::getReferenceDate)
                .collect(java.util.stream.Collectors.toSet());

        Instant now = Instant.now();
        for (var e : entries) {
            if (e.referenceDate() == null) continue;
            // Normalize to YYYY-MM-DD (brapi may return full ISO datetime)
            String refDate = e.referenceDate().length() > 10 ? e.referenceDate().substring(0, 10) : e.referenceDate();
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
}
