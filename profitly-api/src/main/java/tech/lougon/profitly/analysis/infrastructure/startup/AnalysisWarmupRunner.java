package tech.lougon.profitly.analysis.infrastructure.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.application.service.AnalysisService;
import tech.lougon.profitly.ticker.application.service.TickerService;

@Component
public class AnalysisWarmupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AnalysisWarmupRunner.class);
    private static final long DELAY_MS = 500;

    private final TickerService tickerService;
    private final AnalysisService analysisService;

    public AnalysisWarmupRunner(TickerService tickerService, AnalysisService analysisService) {
        this.tickerService = tickerService;
        this.analysisService = analysisService;
    }

    @Override
    public void run(ApplicationArguments args) {
        warmupAsync();
    }

    @Async
    public void warmupAsync() {
        var tickers = tickerService.findAll();
        log.info("Starting analysis warmup for {} tickers", tickers.size());

        int success = 0, failed = 0;
        for (var ticker : tickers) {
            try {
                analysisService.getAnalysis(ticker.symbol());
                success++;
                Thread.sleep(DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Warmup failed for {}: {}", ticker.symbol(), e.getMessage());
                failed++;
            }
        }

        log.info("Analysis warmup complete — {} ok, {} failed", success, failed);
    }
}
