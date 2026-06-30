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
    public void syncStocksEveryHour() {
        List<BrapiTickerListResponse.BrapiTicker> tickers = fetchAllTickers();
        log.info("Starting hourly sync for {} tickers (stocks + FIIs)", tickers.size());
        sync(tickers);
    }

    private List<BrapiTickerListResponse.BrapiTicker> fetchAllTickers() {
        return brapiStockClient.fetchTickerList().stocks().stream()
                .filter(t -> "stock".equalsIgnoreCase(t.type()) || "fii".equalsIgnoreCase(t.type()))
                .toList();
    }

    private void sync(List<BrapiTickerListResponse.BrapiTicker> tickers) {
        int success = 0;
        int failure = 0;

        for (BrapiTickerListResponse.BrapiTicker ticker : tickers) {
            try {
                stockService.syncFromBrapi(ticker.stock(), ticker.type().toLowerCase());
                success++;
            } catch (Exception e) {
                log.warn("Failed to sync {}: {}", ticker.stock(), e.getMessage());
                failure++;
            }
        }

        log.info("Sync finished — success: {}, failure: {}", success, failure);
    }
}
