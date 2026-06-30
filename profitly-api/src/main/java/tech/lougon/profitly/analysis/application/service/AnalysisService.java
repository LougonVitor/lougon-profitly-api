package tech.lougon.profitly.analysis.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.lougon.profitly.analysis.application.dto.PricePointDTO;
import tech.lougon.profitly.analysis.application.dto.TickerAnalysisDTO;
import tech.lougon.profitly.analysis.domain.model.DividendEvent;
import tech.lougon.profitly.analysis.domain.model.PricePoint;
import tech.lougon.profitly.analysis.domain.model.TickerAnalysis;
import tech.lougon.profitly.analysis.domain.repository.DividendRepository;
import tech.lougon.profitly.analysis.domain.repository.PriceHistoryRepository;
import tech.lougon.profitly.analysis.domain.repository.TickerAnalysisRepository;
import tech.lougon.profitly.analysis.infrastructure.client.BrapiAnalysisClient;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiStatisticsResponse;
import tech.lougon.profitly.ticker.application.service.TickerService;

import java.math.BigDecimal;
import java.time.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);
    private static final Duration STATS_TTL = Duration.ofHours(6);
    private static final Duration DIVIDENDS_TTL = Duration.ofHours(24);

    private final TickerService tickerService;
    private final TickerAnalysisRepository analysisRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final DividendRepository dividendRepository;
    private final BrapiAnalysisClient brapiClient;

    public AnalysisService(TickerService tickerService,
                           TickerAnalysisRepository analysisRepository,
                           PriceHistoryRepository priceHistoryRepository,
                           DividendRepository dividendRepository,
                           BrapiAnalysisClient brapiClient) {
        this.tickerService = tickerService;
        this.analysisRepository = analysisRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.dividendRepository = dividendRepository;
        this.brapiClient = brapiClient;
    }

    public TickerAnalysisDTO getAnalysis(String symbol) {
        var ticker = tickerService.findBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("Ticker not found: " + symbol));

        TickerAnalysis stats = resolveStats(symbol);
        List<DividendEvent> dividends = resolveDividends(symbol, stats);

        return TickerAnalysisDTO.of(ticker, stats, dividends);
    }

    public List<PricePointDTO> getPriceHistory(String symbol, String range) {
        LocalDate from = resolveFromDate(range);
        LocalDate to = LocalDate.now();

        Optional<LocalDate> latestInDb = priceHistoryRepository.findLatestDateBySymbol(symbol);
        boolean needsFetch = latestInDb.isEmpty() || latestInDb.get().isBefore(to.minusDays(1));

        if (needsFetch) {
            log.info("Fetching price history for {} range={}", symbol, range);
            String brapiRange = toBrapiRange(range);
            var bars = brapiClient.fetchHistory(symbol, brapiRange);
            if (!bars.isEmpty()) {
                List<PricePoint> points = bars.stream()
                        .filter(b -> b.date() != null && b.close() != null)
                        .map(b -> new PricePoint(
                                symbol,
                                Instant.ofEpochSecond(b.date()).atZone(ZoneOffset.UTC).toLocalDate(),
                                b.open() != null ? BigDecimal.valueOf(b.open()) : null,
                                b.high() != null ? BigDecimal.valueOf(b.high()) : null,
                                b.low() != null ? BigDecimal.valueOf(b.low()) : null,
                                BigDecimal.valueOf(b.close()),
                                b.adjustedClose() != null ? BigDecimal.valueOf(b.adjustedClose()) : null,
                                b.volume()
                        ))
                        .toList();
                priceHistoryRepository.saveAll(points);
            }
        }

        return priceHistoryRepository.findBySymbolAndDateBetween(symbol, from, to)
                .stream().map(PricePointDTO::from).toList();
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private TickerAnalysis resolveStats(String symbol) {
        return analysisRepository.findBySymbol(symbol)
                .filter(a -> !isStale(a.syncedAt(), STATS_TTL))
                .orElseGet(() -> {
                    log.info("Fetching statistics for {}", symbol);
                    return brapiClient.fetchStatistics(symbol)
                            .map(data -> buildAndSave(symbol, data))
                            .orElseGet(() -> buildEmpty(symbol));
                });
    }

    private List<DividendEvent> resolveDividends(String symbol, TickerAnalysis stats) {
        boolean dividendsStale = stats.dividendsSyncedAt() == null
                || isStale(stats.dividendsSyncedAt(), DIVIDENDS_TTL);

        if (dividendsStale) {
            log.info("Fetching dividends for {}", symbol);
            var raw = brapiClient.fetchDividends(symbol);
            List<DividendEvent> events = raw.stream()
                    .map(d -> new DividendEvent(
                            symbol, d.assetIssued(), d.paymentDate(), d.rate(),
                            d.relatedTo(), d.approvedOn(), d.label(),
                            d.lastDatePrior(), d.remarks()
                    ))
                    .toList();

            dividendRepository.deleteBySymbol(symbol);
            if (!events.isEmpty()) dividendRepository.saveAll(events);

            // Update dividends sync timestamp
            TickerAnalysis updated = new TickerAnalysis(
                    stats.symbol(), stats.trailingPE(), stats.priceToBook(),
                    stats.dividendYield(), stats.beta(), stats.earningsPerShare(),
                    stats.forwardPE(), stats.pegRatio(), stats.enterpriseToRevenue(),
                    stats.enterpriseToEbitda(), stats.marketCap(), stats.enterpriseValue(),
                    stats.bookValue(), stats.weekChange52(), stats.profitMargins(),
                    stats.sharesOutstanding(), stats.floatShares(),
                    stats.lastDividendValue(), stats.lastDividendDate(),
                    stats.syncedAt(), Instant.now()
            );
            analysisRepository.save(updated);
            return events;
        }

        return dividendRepository.findBySymbol(symbol);
    }

    private TickerAnalysis buildAndSave(String symbol, BrapiStatisticsResponse.Data data) {
        TickerAnalysis analysis = new TickerAnalysis(
                symbol,
                data.trailingPE(),
                data.priceToBook(),
                data.dividendYield(),
                data.beta(),
                data.earningsPerShare() != null ? data.earningsPerShare() : data.trailingEps(),
                data.forwardPE(),
                data.pegRatio(),
                data.enterpriseToRevenue(),
                data.enterpriseToEbitda(),
                data.marketCap(),
                data.enterpriseValue(),
                data.bookValue(),
                data.weekChange52(),
                data.profitMargins(),
                data.sharesOutstanding(),
                data.floatShares(),
                data.lastDividendValue(),
                data.lastDividendDate(),
                Instant.now(),
                null
        );
        return analysisRepository.save(analysis);
    }

    private TickerAnalysis buildEmpty(String symbol) {
        TickerAnalysis empty = new TickerAnalysis(
                symbol, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                Instant.now(), null
        );
        return analysisRepository.save(empty);
    }

    private boolean isStale(Instant syncedAt, Duration ttl) {
        return syncedAt == null || Instant.now().isAfter(syncedAt.plus(ttl));
    }

    private LocalDate resolveFromDate(String range) {
        LocalDate today = LocalDate.now();
        return switch (range) {
            case "1m"  -> today.minusMonths(1);
            case "3m"  -> today.minusMonths(3);
            case "6m"  -> today.minusMonths(6);
            case "1y"  -> today.minusYears(1);
            case "2y"  -> today.minusYears(2);
            case "5y"  -> today.minusYears(5);
            case "10y" -> today.minusYears(10);
            case "max" -> LocalDate.of(2000, 1, 1);
            default    -> today.minusYears(1);
        };
    }

    private String toBrapiRange(String range) {
        return switch (range) {
            case "1m"  -> "1mo";
            case "3m"  -> "3mo";
            case "6m"  -> "6mo";
            case "1y"  -> "1y";
            case "2y"  -> "2y";
            case "5y"  -> "5y";
            case "10y" -> "10y";
            case "max" -> "max";
            default    -> "5y";
        };
    }
}
