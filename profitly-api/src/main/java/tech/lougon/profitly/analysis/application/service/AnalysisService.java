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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final TickerService tickerService;
    private final TickerAnalysisRepository analysisRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final DividendRepository dividendRepository;
    private final BrapiAnalysisClient brapiClient;
    private final tech.lougon.profitly.analysis.infrastructure.persistence.JpaStockSplitEventRepository splitRepo;

    public AnalysisService(TickerService tickerService,
                           TickerAnalysisRepository analysisRepository,
                           PriceHistoryRepository priceHistoryRepository,
                           DividendRepository dividendRepository,
                           BrapiAnalysisClient brapiClient,
                           tech.lougon.profitly.analysis.infrastructure.persistence.JpaStockSplitEventRepository splitRepo) {
        this.tickerService = tickerService;
        this.analysisRepository = analysisRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.dividendRepository = dividendRepository;
        this.brapiClient = brapiClient;
        this.splitRepo = splitRepo;
    }

    public TickerAnalysisDTO getAnalysis(String symbol) {
        var ticker = tickerService.findBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("Ticker not found: " + symbol));

        TickerAnalysis stats = analysisRepository.findBySymbol(symbol)
                .orElseGet(() -> emptyAnalysis(symbol));
        List<DividendEvent> dividends = dividendRepository.findBySymbol(symbol);
        Map<Integer, Double> historicalDy = computeHistoricalDyByYear(symbol, dividends);

        return TickerAnalysisDTO.of(ticker, stats, dividends, historicalDy);
    }

    public void forceSync(String symbol) {
        var tickerOpt = tickerService.findBySymbol(symbol);
        if (tickerOpt.isEmpty()) {
            log.warn("forceSync: ticker not found {}", symbol);
            return;
        }
        var ticker = tickerOpt.get();

        log.info("Syncing analysis for {}", symbol);

        var statsData     = brapiClient.fetchStatistics(symbol);
        var financialData = brapiClient.fetchFinancialData(symbol);
        TickerAnalysis stats = buildAndSave(symbol, statsData.orElse(null), financialData.orElse(null));

        boolean isFii = "FII".equalsIgnoreCase(ticker.assetType())
                || "FII".equalsIgnoreCase(ticker.subType());

        List<DividendEvent> events;
        if (isFii) {
            events = brapiClient.fetchFiiDividends(symbol).stream()
                    .map(d -> new DividendEvent(symbol, null, d.paymentDate(), d.rate(),
                            d.relatedTo(), d.approvedOn(), d.label(), d.lastDatePrior(), d.remarks()))
                    .toList();
        } else {
            events = brapiClient.fetchDividends(symbol).stream()
                    .map(d -> new DividendEvent(symbol, d.assetIssued(), d.paymentDate(), d.rate(),
                            d.relatedTo(), d.approvedOn(), d.label(), d.lastDatePrior(), d.remarks()))
                    .toList();
        }

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
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Concurrent dividend refresh for {} — skipping", symbol);
        }
    }

    private TickerAnalysis emptyAnalysis(String symbol) {
        return new TickerAnalysis(symbol, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
    }

    public void syncPriceHistory(String symbol) {
        log.info("Syncing price history for {}", symbol);

        boolean isFii = tickerService.findBySymbol(symbol)
                .map(t -> "FII".equalsIgnoreCase(t.assetType()) || "FII".equalsIgnoreCase(t.subType()))
                .orElse(false);

        List<PricePoint> points;
        if (isFii) {
            var bars = brapiClient.fetchFiiHistory(symbol, "max");
            points = bars.stream()
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
        } else {
            var bars = brapiClient.fetchHistory(symbol, "max");
            points = bars.stream()
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
        }
        // Insert only bars newer than the last stored one — brapi always returns the
        // full range and re-inserting an existing (symbol, date) row violates the
        // unique constraint, aborting the whole save.
        var latest = priceHistoryRepository.findLatestDateBySymbol(symbol);
        List<PricePoint> newPoints = latest
                .map(last -> points.stream().filter(p -> p.date().isAfter(last)).toList())
                .orElse(points);
        if (!newPoints.isEmpty()) {
            priceHistoryRepository.saveAll(newPoints);
        }
    }

    public List<PricePointDTO> getPriceHistory(String symbol, String range) {
        LocalDate from = resolveFromDate(range);
        LocalDate to = LocalDate.now();
        return priceHistoryRepository.findBySymbolAndDateBetween(symbol, from, to)
                .stream().map(PricePointDTO::from).toList();
    }


    /** Saves statistics + financial-data as a TickerAnalysis row (used by the stock sync scheduler). */
    public TickerAnalysis saveIndicators(String symbol,
                                         BrapiStatisticsResponse.Data statsData,
                                         BrapiFinancialDataResponse.Data financialData) {
        return buildAndSave(symbol, statsData, financialData);
    }

    /** Annual DY% (sum of dividends / average close in the year × 100) for a symbol. */
    public Map<Integer, Double> getHistoricalDyByYear(String symbol) {
        return computeHistoricalDyByYear(symbol, dividendRepository.findBySymbol(symbol));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

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

    /** DY history window in years (limits storage and matches Investidor10 charts). */
    private static final int DY_HISTORY_YEARS = 20;

    /**
     * Computes annual DY% on the split-adjusted basis, matching Investidor10:
     * DY(year) = sum(split-adjusted dividends with ex-date in year) / close of the LAST
     * trading day of that year × 100.
     *
     * price_points closes are split-adjusted retroactively (Yahoo convention), while
     * dividend rates are as-paid — so each rate is divided by the cumulative factor of
     * all splits that happened AFTER its ex-date to put both on the same basis.
     * Years without price data in DB are omitted from the result.
     */
    private Map<Integer, Double> computeHistoricalDyByYear(String symbol, List<DividendEvent> dividends) {
        if (dividends.isEmpty()) return Map.of();

        int minYear = LocalDate.now().getYear() - DY_HISTORY_YEARS;

        var splits = splitRepo.findBySymbol(symbol).stream()
                .filter(s -> s.getFactor() != null && s.getFactor() > 0
                        && s.getLastDatePrior() != null && s.getLastDatePrior().length() >= 10)
                .toList();

        // Sum split-adjusted dividend rates per calendar year (by ex-date)
        Map<Integer, Double> sumByYear = new HashMap<>();
        for (DividendEvent d : dividends) {
            if (d.rate() == null || d.rate() <= 0
                    || d.lastDatePrior() == null || d.lastDatePrior().length() < 10) continue;
            String exDate = d.lastDatePrior().substring(0, 10);
            int year;
            try {
                year = Integer.parseInt(exDate.substring(0, 4));
            } catch (NumberFormatException e) { continue; }
            if (year < minYear) continue;

            double cumFactor = 1.0;
            for (var s : splits) {
                if (s.getLastDatePrior().substring(0, 10).compareTo(exDate) > 0) {
                    cumFactor *= s.getFactor();
                }
            }
            sumByYear.merge(year, d.rate() / cumFactor, Double::sum);
        }
        if (sumByYear.isEmpty()) return Map.of();

        // Close of the last trading day of each year (same adjusted basis as the rates above)
        Map<Integer, Double> closeByYear = priceHistoryRepository.endOfYearCloseBySymbol(symbol);

        Map<Integer, Double> result = new HashMap<>();
        for (Map.Entry<Integer, Double> entry : sumByYear.entrySet()) {
            Double close = closeByYear.get(entry.getKey());
            if (close != null && close > 0) {
                result.put(entry.getKey(), (entry.getValue() / close) * 100.0);
            }
        }
        return result;
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
