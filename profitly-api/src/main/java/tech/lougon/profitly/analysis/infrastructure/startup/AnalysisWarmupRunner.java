package tech.lougon.profitly.analysis.infrastructure.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.application.service.AnalysisService;
import tech.lougon.profitly.ticker.application.service.TickerService;

import java.util.List;

@Component
public class AnalysisWarmupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AnalysisWarmupRunner.class);
    private static final long DELAY_MS = 500;

    private final AnalysisService analysisService;
    private final TickerService tickerService;

    public AnalysisWarmupRunner(AnalysisService analysisService, TickerService tickerService) {
        this.analysisService = analysisService;
        this.tickerService = tickerService;
    }

    @Override
    public void run(ApplicationArguments args) {
        syncAsync();
    }

    @Scheduled(cron = "0 0 18 * * MON-FRI", zone = "America/Sao_Paulo")
    public void scheduledSync() {
        log.info("Daily 18h sync triggered");
        syncAsync();
    }

    @Async
    public void syncAsync() {
        List<String> symbols = tickerService.findAll().stream()
                .map(t -> t.symbol())
                .toList();

        log.info("Starting analysis sync for {} tickers", symbols.size());

        int success = 0, failed = 0;
        for (String symbol : symbols) {
            try {
                analysisService.getAnalysis(symbol);
                success++;
                Thread.sleep(DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Sync failed for {}: {}", symbol, e.getMessage());
                failed++;
            }
        }

        log.info("Analysis sync complete — {} ok, {} failed", success, failed);
    }
}
