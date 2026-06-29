package tech.lougon.profitly.stock.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.stock.application.service.StockQuoteService;

import java.util.List;

@Component
public class StockQuoteSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(StockQuoteSyncScheduler.class);

    private static final List<String> TICKERS = List.of(
            "PETR4", "VALE3", "ITUB4", "BBAS3", "ABEV3",
            "WEGE3", "RENT3", "BBDC4", "EGIE3", "TAEE11"
    );

    private final StockQuoteService stockService;

    public StockQuoteSyncScheduler(StockQuoteService stockService) {
        this.stockService = stockService;
    }

    @Scheduled(cron = "${profitly.scheduler.stock-sync-hourly-cron}")
    public void syncStocksEveryHour() {
        log.info("Starting hourly stock sync for {} tickers", TICKERS.size());
        sync();
    }

    private void sync() {
        int success = 0;
        int failure = 0;

        for (String ticker : TICKERS) {
            try {
                stockService.syncFromBrapi(ticker);
                log.info("Synced ticker: {}", ticker);
                success++;
            } catch (Exception e) {
                log.error("Failed to sync ticker {}: {}", ticker, e.getMessage());
                failure++;
            }
        }

        log.info("Stock sync finished — success: {}, failure: {}", success, failure);
    }
}