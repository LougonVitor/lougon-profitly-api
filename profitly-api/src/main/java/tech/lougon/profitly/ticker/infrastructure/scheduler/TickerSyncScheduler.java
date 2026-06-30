package tech.lougon.profitly.ticker.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.ticker.application.service.TickerService;

@Component
public class TickerSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(TickerSyncScheduler.class);
    private final TickerService tickerService;

    public TickerSyncScheduler(TickerService tickerService) {
        this.tickerService = tickerService;
    }

    public void syncAll() {
        log.info("Starting ticker sync from /api/v2/tickers");
        tickerService.syncAll();
        log.info("Ticker sync finished");
    }
}
