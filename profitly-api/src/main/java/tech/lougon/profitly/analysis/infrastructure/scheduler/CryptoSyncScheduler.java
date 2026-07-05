package tech.lougon.profitly.analysis.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.domain.model.PricePoint;
import tech.lougon.profitly.analysis.domain.repository.PriceHistoryRepository;
import tech.lougon.profitly.analysis.infrastructure.client.BrapiAnalysisClient;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiCryptoResponse;
import tech.lougon.profitly.analysis.infrastructure.persistence.CryptoCoinJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.CryptoQuoteJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaCryptoCoinRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaCryptoQuoteRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.JpaTickerRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.TickerJpaEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Syncs crypto data from brapi:
 *  1. /api/v2/crypto/available — catalog of coin symbols (crypto_coins table)
 *  2. /api/v2/crypto?coin=...&currency=BRL — quote data in batches (crypto_quotes table)
 *  3. /api/v2/crypto?coin=...&range=max|3mo&interval=1d — daily price history stored in
 *     price_points (same table the generic /api/analysis/{symbol}/history chart reads from).
 *     Coins without any history get a full range=max backfill; the rest get an incremental
 *     3-month fetch, inserting only bars newer than the latest stored date.
 */
@Component
public class CryptoSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(CryptoSyncScheduler.class);
    private static final int BATCH_SIZE = 20;
    /** range=max history payloads are large (~10y of daily bars per coin), so smaller batches. */
    private static final int FULL_HISTORY_BATCH_SIZE = 5;
    private static final int INCREMENTAL_HISTORY_BATCH_SIZE = 20;

    private final BrapiAnalysisClient brapiClient;
    private final JpaCryptoCoinRepository coinRepo;
    private final JpaCryptoQuoteRepository quoteRepo;
    private final JpaTickerRepository tickerRepo;
    private final PriceHistoryRepository priceHistoryRepository;

    public CryptoSyncScheduler(BrapiAnalysisClient brapiClient,
                                JpaCryptoCoinRepository coinRepo,
                                JpaCryptoQuoteRepository quoteRepo,
                                JpaTickerRepository tickerRepo,
                                PriceHistoryRepository priceHistoryRepository) {
        this.brapiClient = brapiClient;
        this.coinRepo = coinRepo;
        this.quoteRepo = quoteRepo;
        this.tickerRepo = tickerRepo;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @Value("${profitly.sync.on-startup:false}")
    private boolean syncOnStartup;

    /** TEMP dev flag: re-syncs crypto on every restart while the crypto screen is being built.
     *  Disable after development to save brapi PRO requests. */
    @Value("${profitly.sync.crypto-on-startup:false}")
    private boolean cryptoSyncOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void syncOnStartup() {
        if (!syncOnStartup && !cryptoSyncOnStartup) return;
        log.info("Running crypto startup sync");
        syncAll();
    }

    @Scheduled(cron = "0 50 19 * * *", zone = "America/Sao_Paulo")
    public void syncAll() {
        // Step 1: coin catalog
        List<String> coins = brapiClient.fetchCryptoAvailable();
        if (coins.isEmpty()) {
            log.warn("Crypto available list returned 0 coins — skipping crypto sync");
            return;
        }

        Instant now = Instant.now();
        for (String coin : coins) {
            coinRepo.save(new CryptoCoinJpaEntity(coin, now));
        }
        log.info("Crypto coin catalog synced: {} coins", coins.size());

        // Step 2: quotes in batches of 20
        int quotesSynced = 0;
        for (List<String> batch : partition(coins, BATCH_SIZE)) {
            List<BrapiCryptoResponse.CryptoQuote> quotes =
                    brapiClient.fetchCryptoQuotes(String.join(",", batch));
            for (BrapiCryptoResponse.CryptoQuote quote : quotes) {
                try {
                    saveQuote(quote);
                    upsertTicker(quote);
                    quotesSynced++;
                } catch (Exception e) {
                    log.warn("Failed to save crypto quote for {}: {}", quote.coin(), e.getMessage());
                }
            }
        }
        log.info("Crypto sync complete: {}/{} quotes synced", quotesSynced, coins.size());

        // Step 3: daily price history into price_points
        syncPriceHistory(coins);
    }

    private void syncPriceHistory(List<String> coins) {
        List<String> needsFullBackfill = new ArrayList<>();
        List<String> incremental = new ArrayList<>();
        for (String coin : coins) {
            if (priceHistoryRepository.findLatestDateBySymbol(coin).isEmpty()) {
                needsFullBackfill.add(coin);
            } else {
                incremental.add(coin);
            }
        }
        log.info("Crypto price history sync: {} full backfill, {} incremental",
                needsFullBackfill.size(), incremental.size());

        int barsSaved = 0;
        for (List<String> batch : partition(needsFullBackfill, FULL_HISTORY_BATCH_SIZE)) {
            barsSaved += fetchAndStoreHistory(batch, "max");
        }
        for (List<String> batch : partition(incremental, INCREMENTAL_HISTORY_BATCH_SIZE)) {
            barsSaved += fetchAndStoreHistory(batch, "3mo");
        }
        log.info("Crypto price history sync complete: {} bars saved", barsSaved);
    }

    private int fetchAndStoreHistory(List<String> batch, String range) {
        List<BrapiCryptoResponse.CryptoQuote> quotes =
                brapiClient.fetchCryptoQuotes(String.join(",", batch), range, "1d");
        int saved = 0;
        for (BrapiCryptoResponse.CryptoQuote quote : quotes) {
            try {
                saved += storeHistory(quote);
            } catch (Exception e) {
                log.warn("Failed to save crypto price history for {}: {}", quote.coin(), e.getMessage());
            }
        }
        return saved;
    }

    /** Inserts only bars newer than the latest stored date (price_points has a unique symbol+date). */
    private int storeHistory(BrapiCryptoResponse.CryptoQuote quote) {
        if (quote.historicalDataPrice() == null || quote.historicalDataPrice().isEmpty()) return 0;

        LocalDate latest = priceHistoryRepository.findLatestDateBySymbol(quote.coin()).orElse(null);
        List<PricePoint> points = quote.historicalDataPrice().stream()
                .filter(b -> b.date() != null && b.close() != null)
                .map(b -> new PricePoint(
                        quote.coin(),
                        Instant.ofEpochSecond(b.date()).atZone(ZoneOffset.UTC).toLocalDate(),
                        b.open() != null ? BigDecimal.valueOf(b.open()) : null,
                        b.high() != null ? BigDecimal.valueOf(b.high()) : null,
                        b.low() != null ? BigDecimal.valueOf(b.low()) : null,
                        BigDecimal.valueOf(b.close()),
                        b.adjustedClose() != null ? BigDecimal.valueOf(b.adjustedClose()) : null,
                        b.volume() != null ? Math.round(b.volume()) : null
                ))
                .filter(p -> latest == null || p.date().isAfter(latest))
                .toList();

        if (!points.isEmpty()) {
            priceHistoryRepository.saveAll(points);
        }
        return points.size();
    }

    private void saveQuote(BrapiCryptoResponse.CryptoQuote quote) {
        CryptoQuoteJpaEntity entity = quoteRepo.findById(quote.coin())
                .orElseGet(() -> { var e = new CryptoQuoteJpaEntity(); e.setCoin(quote.coin()); return e; });

        entity.setCoinName(quote.coinName());
        entity.setCurrency(quote.currency());
        entity.setImageUrl(quote.coinImageUrl());
        entity.setPrice(quote.regularMarketPrice());
        entity.setUsdToBrlRate(quote.currencyRateFromUSD());
        entity.setChangeValue(quote.regularMarketChange());
        entity.setChangePercent(quote.regularMarketChangePercent());
        entity.setDayHigh(quote.regularMarketDayHigh());
        entity.setDayLow(quote.regularMarketDayLow());
        entity.setVolume(quote.regularMarketVolume());
        entity.setMarketCap(quote.marketCap());
        entity.setMarketTime(parseInstant(quote.regularMarketTime()));
        entity.setSyncedAt(Instant.now());
        quoteRepo.save(entity);
    }

    /** Upserts the coin into the tickers table so it appears in search. */
    private void upsertTicker(BrapiCryptoResponse.CryptoQuote quote) {
        TickerJpaEntity ticker = tickerRepo.findBySymbol(quote.coin())
                .orElseGet(TickerJpaEntity::new);

        String name = quote.coinName() != null ? quote.coinName() : quote.coin();
        ticker.setSymbol(quote.coin());
        ticker.setName(name);
        ticker.setLongName(name);
        ticker.setAssetType("crypto");
        ticker.setSubType("crypto");
        ticker.setCurrency(quote.currency());
        ticker.setLogoUrl(quote.coinImageUrl());
        ticker.setIsActive(true);
        if (quote.regularMarketPrice() != null) {
            ticker.setLastPrice(BigDecimal.valueOf(quote.regularMarketPrice()));
        }
        ticker.setSyncedAt(Instant.now());
        tickerRepo.save(ticker);
    }

    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return Instant.parse(iso);
        } catch (Exception e) {
            return null;
        }
    }

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}
