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
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFinancialDataResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiStatisticsResponse;
import tech.lougon.profitly.ticker.application.dto.TickerDTO;
import tech.lougon.profitly.ticker.application.service.TickerService;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        List<DividendEvent> dividends = resolveDividends(symbol, ticker, stats);

        return TickerAnalysisDTO.of(ticker, stats, dividends);
    }

    public List<PricePointDTO> getPriceHistory(String symbol, String range) {
        LocalDate from = resolveFromDate(range);
        LocalDate to = LocalDate.now();

        Optional<LocalDate> latestInDb = priceHistoryRepository.findLatestDateBySymbol(symbol);
        boolean needsFetch = latestInDb.isEmpty() || latestInDb.get().isBefore(to.minusDays(1));

        if (needsFetch) {
            log.info("Fetching price history for {} range={}", symbol, range);
            var bars = brapiClient.fetchHistory(symbol, "max");
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
        Optional<TickerAnalysis> cached = analysisRepository.findBySymbol(symbol);

        // Only use cache if fresh AND has some meaningful data
        if (cached.isPresent() && !isStale(cached.get().syncedAt(), STATS_TTL) && hasData(cached.get())) {
            return cached.get();
        }

        log.info("Fetching statistics for {}", symbol);
        var statsData    = brapiClient.fetchStatistics(symbol);
        var financialData = brapiClient.fetchFinancialData(symbol);

        return buildAndSave(symbol, statsData.orElse(null), financialData.orElse(null));
    }

    private boolean hasData(TickerAnalysis a) {
        return a.trailingPE() != null || a.priceToBook() != null || a.beta() != null
                || a.dividendYield() != null || a.earningsPerShare() != null
                || a.profitMargins() != null;
    }

    private List<DividendEvent> resolveDividends(String symbol, TickerDTO ticker, TickerAnalysis stats) {
        List<DividendEvent> cached = dividendRepository.findBySymbol(symbol);
        boolean dividendsStale = stats.dividendsSyncedAt() == null
                || isStale(stats.dividendsSyncedAt(), DIVIDENDS_TTL)
                || cached.isEmpty();

        if (dividendsStale) {
            log.info("Fetching dividends for {}", symbol);
            boolean isFii = "FII".equalsIgnoreCase(ticker.assetType())
                    || "FII".equalsIgnoreCase(ticker.subType());

            var raw = isFii
                    ? brapiClient.fetchFiiDividends(symbol)
                    : brapiClient.fetchDividends(symbol);

            List<DividendEvent> events = raw.stream()
                    .map(d -> new DividendEvent(
                            symbol, d.assetIssued(), d.paymentDate(), d.rate(),
                            d.relatedTo(), d.approvedOn(), d.label(),
                            d.lastDatePrior(), d.remarks()
                    ))
                    .toList();

            try {
                dividendRepository.replaceAll(symbol, events);

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
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.warn("Concurrent dividend refresh for {} — reading from DB", symbol);
            }
        }

        return cached;
    }

    private TickerAnalysis buildAndSave(String symbol,
                                        BrapiStatisticsResponse.Data s,
                                        BrapiFinancialDataResponse.Data f) {
        // Statistics fields — prefer stats; fallback to financial-data where available
        BigDecimal trailingPE           = s != null ? s.trailingPE() : null;
        BigDecimal priceToBook          = s != null ? s.priceToBook() : null;
        BigDecimal dividendYield        = s != null ? s.dividendYield() : null;
        BigDecimal beta                 = s != null ? s.beta() : null;
        BigDecimal earningsPerShare     = s != null
                ? (s.earningsPerShare() != null ? s.earningsPerShare() : s.trailingEps())
                : null;
        BigDecimal forwardPE            = s != null ? s.forwardPE() : null;
        BigDecimal pegRatio             = s != null ? s.pegRatio() : null;
        BigDecimal enterpriseToRevenue  = s != null ? s.enterpriseToRevenue() : null;
        BigDecimal enterpriseToEbitda   = s != null ? s.enterpriseToEbitda() : null;
        Long marketCap                  = s != null ? s.marketCap() : null;
        Long enterpriseValue            = s != null ? s.enterpriseValue() : null;
        BigDecimal bookValue            = s != null ? s.bookValue() : null;
        BigDecimal weekChange52         = s != null ? s.weekChange52() : null;
        Long sharesOutstanding          = s != null ? s.sharesOutstanding() : null;
        Long floatShares                = s != null ? s.floatShares() : null;
        BigDecimal lastDividendValue    = s != null ? s.lastDividendValue() : null;
        String lastDividendDate         = s != null ? s.lastDividendDate() : null;

        // Profit margins: prefer stats, fallback to financial-data
        BigDecimal profitMargins = s != null && s.profitMargins() != null
                ? s.profitMargins()
                : (f != null ? f.profitMargins() : null);

        // EV/EBITDA: compute when API doesn't supply it
        if (enterpriseToEbitda == null && enterpriseValue != null
                && f != null && f.ebitda() != null && f.ebitda() != 0) {
            enterpriseToEbitda = BigDecimal.valueOf(enterpriseValue)
                    .divide(BigDecimal.valueOf(f.ebitda()), 2, RoundingMode.HALF_UP);
        }

        // EV/Receita: compute when API doesn't supply it
        if (enterpriseToRevenue == null && enterpriseValue != null
                && f != null && f.totalRevenue() != null && f.totalRevenue() != 0) {
            enterpriseToRevenue = BigDecimal.valueOf(enterpriseValue)
                    .divide(BigDecimal.valueOf(f.totalRevenue()), 2, RoundingMode.HALF_UP);
        }

        TickerAnalysis analysis = new TickerAnalysis(
                symbol, trailingPE, priceToBook, dividendYield, beta, earningsPerShare,
                forwardPE, pegRatio, enterpriseToRevenue, enterpriseToEbitda,
                marketCap, enterpriseValue, bookValue, weekChange52, profitMargins,
                sharesOutstanding, floatShares, lastDividendValue, lastDividendDate,
                Instant.now(), null
        );
        return analysisRepository.save(analysis);
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
}
