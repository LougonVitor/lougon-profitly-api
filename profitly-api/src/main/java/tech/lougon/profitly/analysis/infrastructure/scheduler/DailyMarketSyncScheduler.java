package tech.lougon.profitly.analysis.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.infrastructure.startup.IbovespaSyncScheduler;
import tech.lougon.profitly.analysis.infrastructure.startup.PriceHistorySyncScheduler;
import tech.lougon.profitly.ticker.infrastructure.scheduler.TickerSyncScheduler;

/**
 * Runs the market-data pipeline (13h/18h BRT) from one clock. Ordering matters:
 * the ticker catalog is refreshed before the specialised synchronizers consume it,
 * while a single trigger prevents the independent jobs from competing for BRAPI limits.
 */
@Component
public class DailyMarketSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyMarketSyncScheduler.class);

    private final TickerSyncScheduler tickerSync;
    private final FiiIndicatorSyncScheduler fiiSync;
    private final FundSyncScheduler fundSync;
    private final TreasurySyncScheduler treasurySync;
    private final CryptoSyncScheduler cryptoSync;
    private final StockAnalysisSyncScheduler stockSync;
    private final MacroIndexSyncScheduler macroSync;
    private final PriceHistorySyncScheduler walletPriceSync;
    private final IbovespaSyncScheduler ibovespaSync;

    public DailyMarketSyncScheduler(TickerSyncScheduler tickerSync,
                                    FiiIndicatorSyncScheduler fiiSync,
                                    FundSyncScheduler fundSync,
                                    TreasurySyncScheduler treasurySync,
                                    CryptoSyncScheduler cryptoSync,
                                    StockAnalysisSyncScheduler stockSync,
                                    MacroIndexSyncScheduler macroSync,
                                    PriceHistorySyncScheduler walletPriceSync,
                                    IbovespaSyncScheduler ibovespaSync) {
        this.tickerSync = tickerSync;
        this.fiiSync = fiiSync;
        this.fundSync = fundSync;
        this.treasurySync = treasurySync;
        this.cryptoSync = cryptoSync;
        this.stockSync = stockSync;
        this.macroSync = macroSync;
        this.walletPriceSync = walletPriceSync;
        this.ibovespaSync = ibovespaSync;
    }

    // News runs on its own 30-min cadence (NewsSyncScheduler) — not part of this BRAPI pipeline.
    @Scheduled(cron = "0 0 13,18 * * *", zone = "America/Sao_Paulo")
    public void syncAll() {
        log.info("Market sync started");
        run("tickers", tickerSync::scheduledSync);
        run("FIIs", fiiSync::syncAll);
        run("funds", fundSync::syncAll);
        run("treasury", treasurySync::syncAll);
        run("crypto", cryptoSync::syncAll);
        run("stocks", stockSync::syncAll);
        run("macro indexes", macroSync::syncAll);
        run("wallet price history and dividends", walletPriceSync::scheduledSync);
        run("Ibovespa", ibovespaSync::syncAllRanges);
        log.info("Market sync finished");
    }

    private void run(String name, Runnable task) {
        try {
            log.info("Market sync: starting {}", name);
            task.run();
            log.info("Market sync: finished {}", name);
        } catch (Exception e) {
            // One failed provider/module must not stop the rest of the run.
            log.error("Market sync: {} failed", name, e);
        }
    }
}
