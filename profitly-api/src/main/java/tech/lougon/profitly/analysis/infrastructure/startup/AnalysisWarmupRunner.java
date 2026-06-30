package tech.lougon.profitly.analysis.infrastructure.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.application.service.AnalysisService;

@Component
public class AnalysisWarmupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AnalysisWarmupRunner.class);
    private static final long DELAY_MS = 500;

    private final AnalysisService analysisService;

    public AnalysisWarmupRunner(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @Override
    public void run(ApplicationArguments args) {
        warmupAsync();
    }

    // TODO: expand to tickerService.findAll() once BBAS3 is fully validated
    private static final java.util.List<String> PILOT_SYMBOLS = java.util.List.of("BBAS3");

    @Async
    public void warmupAsync() {
        log.info("Starting analysis warmup for {} tickers", PILOT_SYMBOLS.size());

        int success = 0, failed = 0;
        for (String symbol : PILOT_SYMBOLS) {
            try {
                analysisService.getAnalysis(symbol);
                success++;
                Thread.sleep(DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Warmup failed for {}: {}", symbol, e.getMessage());
                failed++;
            }
        }

        log.info("Analysis warmup complete — {} ok, {} failed", success, failed);
    }
}
