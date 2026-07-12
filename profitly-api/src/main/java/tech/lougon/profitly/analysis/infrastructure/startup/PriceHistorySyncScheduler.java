package tech.lougon.profitly.analysis.infrastructure.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.application.service.AnalysisService;
import tech.lougon.profitly.analysis.domain.repository.PriceHistoryRepository;
import tech.lougon.profitly.wallet.infrastructure.persistence.JpaWalletPositionRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedSet;

@Component
public class PriceHistorySyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(PriceHistorySyncScheduler.class);
    private static final long DELAY_MS = 400;

    private final AnalysisService analysisService;
    private final JpaWalletPositionRepository positionRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public PriceHistorySyncScheduler(AnalysisService analysisService,
                                     JpaWalletPositionRepository positionRepository,
                                     PriceHistoryRepository priceHistoryRepository) {
        this.analysisService = analysisService;
        this.positionRepository = positionRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    // Dev-only flag — syncs wallet tickers' price history right after boot
    @Value("${profitly.sync.wallet-prices-on-startup:false}")
    private boolean walletPricesOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void syncOnStartup() {
        if (!walletPricesOnStartup) return;
        log.info("Running wallet price history startup sync");
        syncAsync();
    }

    // Runs at 19:02 daily — syncs portfolio tickers (keep price chart up-to-date)
    @Scheduled(cron = "0 2 19 * * *", zone = "America/Sao_Paulo")
    public void scheduledSync() {
        log.info("Daily price history sync triggered");
        syncAsync();
    }

    // Runs at 02:30 nightly — fills gaps for tickers with dividends but no price history
    // This enables accurate historical DY% on the analysis page without any live brapi call per user
    @Scheduled(cron = "0 30 2 * * *", zone = "America/Sao_Paulo")
    public void scheduledDividendTickerSync() {
        log.info("Nightly dividend-ticker price history backfill triggered");
        syncDividendTickersAsync();
    }

    @Async
    public void syncAsync() {
        List<String> symbols = positionRepository.findDistinctTickers();
        log.info("Starting price history sync for {} portfolio tickers", symbols.size());
        runSync(symbols);
    }

    @Async
    public void syncDividendTickersAsync() {
        List<String> missing = priceHistoryRepository.findSymbolsWithDividendsButNoPriceHistory();
        log.info("Starting price history backfill for {} dividend tickers without history", missing.size());
        runSync(missing);
    }

    private void runSync(List<String> symbols) {
        int success = 0, failed = 0;
        for (String symbol : symbols) {
            try {
                analysisService.syncPriceHistory(symbol);
                success++;
                Thread.sleep(DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Price history sync failed for {}: {}", symbol, e.getMessage());
                failed++;
            }
        }
        log.info("Price history sync complete — {} ok, {} failed", success, failed);
    }
}
