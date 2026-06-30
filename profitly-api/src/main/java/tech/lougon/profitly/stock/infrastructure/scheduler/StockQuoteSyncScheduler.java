package tech.lougon.profitly.stock.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.stock.application.service.StockQuoteService;
import tech.lougon.profitly.stock.infrastructure.client.BrapiStockClient;
import tech.lougon.profitly.stock.infrastructure.client.dto.BrapiTickerListResponse;

import java.util.List;

@Component
public class StockQuoteSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(StockQuoteSyncScheduler.class);
    private final StockQuoteService stockService;
    private final BrapiStockClient brapiStockClient;

    public StockQuoteSyncScheduler(StockQuoteService stockService, BrapiStockClient brapiStockClient) {
        this.stockService = stockService;
        this.brapiStockClient = brapiStockClient;
    }

    @Scheduled(cron = "${profitly.scheduler.stock-sync-hourly-cron}")
    public void syncEveryHour() {
        List<String> stockTickers = fetchStockTickers();
        log.info("Starting hourly sync — {} stocks", stockTickers.size());
        sync(stockTickers, "stock");

        List<String> fiiTickers = brapiStockClient.fetchAllFiiSymbols();
        log.info("Syncing {} FIIs", fiiTickers.size());
        sync(fiiTickers, "fii");
    }

    private List<String> fetchStockTickers() {
        return brapiStockClient.fetchTickerList().stocks().stream()
                .filter(t -> "stock".equalsIgnoreCase(t.type()))
                .map(BrapiTickerListResponse.BrapiTicker::stock)
                .toList();
    }

    private void sync(List<String> tickers, String assetType) {
        int success = 0;
        int failure = 0;

        for (String ticker : tickers) {
            try {
                stockService.syncFromBrapi(ticker, assetType);
                success++;
            } catch (Exception e) {
                log.warn("Failed to sync {} ({}): {}", ticker, assetType, e.getMessage());
                failure++;
            }
        }

        log.info("Sync [{}] finished — success: {}, failure: {}", assetType, success, failure);
    }
}
