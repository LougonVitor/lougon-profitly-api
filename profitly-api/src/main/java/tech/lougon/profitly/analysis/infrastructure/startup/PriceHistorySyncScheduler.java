package tech.lougon.profitly.analysis.infrastructure.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.application.service.AnalysisService;
import tech.lougon.profitly.wallet.infrastructure.persistence.JpaWalletPositionRepository;

import java.util.List;

@Component
public class PriceHistorySyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(PriceHistorySyncScheduler.class);
    private static final long DELAY_MS = 400;

    private final AnalysisService analysisService;
    private final JpaWalletPositionRepository positionRepository;

    public PriceHistorySyncScheduler(AnalysisService analysisService,
                                     JpaWalletPositionRepository positionRepository) {
        this.analysisService = analysisService;
        this.positionRepository = positionRepository;
    }

    // Runs at 19:00 daily — after ticker sync, before analysis sync
    @Scheduled(cron = "0 2 19 * * *", zone = "America/Sao_Paulo")
    public void scheduledSync() {
        log.info("Daily price history sync triggered");
        syncAsync();
    }

    @Async
    public void syncAsync() {
        List<String> symbols = positionRepository.findDistinctTickers();
        log.info("Starting price history sync for {} portfolio tickers", symbols.size());

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
