package tech.lougon.profitly.analysis.application.service;

import org.springframework.stereotype.Service;
import tech.lougon.profitly.analysis.application.dto.CryptoAnalysisDTO;
import tech.lougon.profitly.analysis.domain.model.PricePoint;
import tech.lougon.profitly.analysis.domain.repository.PriceHistoryRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.CryptoQuoteJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaCryptoQuoteRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Computes advanced crypto indicators from the daily closes stored in price_points.
 * Everything is served from the database — no brapi call happens here.
 */
@Service
public class CryptoAnalysisService {

    /** Crypto trades every day, so annualization uses 365 periods. */
    private static final double PERIODS_PER_YEAR = 365.0;

    private final JpaCryptoQuoteRepository quoteRepo;
    private final PriceHistoryRepository priceHistoryRepository;

    public CryptoAnalysisService(JpaCryptoQuoteRepository quoteRepo,
                                 PriceHistoryRepository priceHistoryRepository) {
        this.quoteRepo = quoteRepo;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    public Optional<CryptoAnalysisDTO> getAnalysis(String coin) {
        String symbol = coin.toUpperCase();
        CryptoQuoteJpaEntity quote = quoteRepo.findById(symbol).orElse(null);
        if (quote == null) return Optional.empty();

        List<CryptoQuoteJpaEntity> ranking = quoteRepo.findAllByOrderByVolumeDesc();
        Integer volumeRank = null;
        for (int i = 0; i < ranking.size(); i++) {
            if (symbol.equals(ranking.get(i).getCoin())) { volumeRank = i + 1; break; }
        }

        List<PricePoint> history = priceHistoryRepository
                .findBySymbolAndDateBetween(symbol, LocalDate.of(2000, 1, 1), LocalDate.now());

        return Optional.of(build(quote, volumeRank, ranking.size(), history));
    }

    private CryptoAnalysisDTO build(CryptoQuoteJpaEntity q, Integer volumeRank, int totalCoins,
                                    List<PricePoint> history) {
        Double priceUsd = null;
        if (q.getPrice() != null && q.getUsdToBrlRate() != null && q.getUsdToBrlRate() > 0) {
            priceUsd = q.getPrice() / q.getUsdToBrlRate();
        }

        // history comes ordered by date asc; closes drive every indicator
        List<PricePoint> bars = history.stream().filter(p -> p.close() != null).toList();
        double[] closes = bars.stream().mapToDouble(p -> p.close().doubleValue()).toArray();
        int n = closes.length;

        Double high52w = null, low52w = null, position52w = null;
        Double athPrice = null;
        LocalDate athDate = null;
        Double distFromAth = null;
        Map<String, Double> returns = new LinkedHashMap<>();
        Double vol30 = null, vol1y = null, drawdown1y = null;
        Double sma50 = null, sma200 = null, vsSma50 = null, vsSma200 = null;
        Integer historyDays = n > 0 ? n : null;
        LocalDate historyStart = n > 0 ? bars.get(0).date() : null;

        if (n > 0) {
            double current = q.getPrice() != null ? q.getPrice() : closes[n - 1];
            LocalDate lastDate = bars.get(n - 1).date();

            // 52-week window uses intraday highs/lows when present
            LocalDate cutoff52 = lastDate.minusWeeks(52);
            double hi = Double.MIN_VALUE, lo = Double.MAX_VALUE;
            for (PricePoint p : bars) {
                if (p.date().isBefore(cutoff52)) continue;
                double h = p.high() != null ? p.high().doubleValue() : p.close().doubleValue();
                double l = p.low() != null ? p.low().doubleValue() : p.close().doubleValue();
                if (h > hi) hi = h;
                if (l < lo) lo = l;
            }
            if (hi > Double.MIN_VALUE) {
                high52w = hi;
                low52w = lo;
                if (hi > lo) position52w = clamp((current - lo) / (hi - lo) * 100.0);
            }

            // all-time high on closes
            for (PricePoint p : bars) {
                double c = p.close().doubleValue();
                if (athPrice == null || c > athPrice) { athPrice = c; athDate = p.date(); }
            }
            if (athPrice != null && athPrice > 0) {
                distFromAth = (current - athPrice) / athPrice * 100.0;
            }

            putReturn(returns, "7d", bars, current, lastDate.minusDays(7));
            putReturn(returns, "1m", bars, current, lastDate.minusMonths(1));
            putReturn(returns, "3m", bars, current, lastDate.minusMonths(3));
            putReturn(returns, "6m", bars, current, lastDate.minusMonths(6));
            putReturn(returns, "ytd", bars, current, lastDate.withDayOfYear(1).minusDays(1));
            putReturn(returns, "1y", bars, current, lastDate.minusYears(1));
            putReturn(returns, "2y", bars, current, lastDate.minusYears(2));
            if (closes[0] > 0 && n > 1) {
                returns.put("max", round2((current - closes[0]) / closes[0] * 100.0));
            }

            vol30 = annualizedVolatility(closes, 30);
            vol1y = annualizedVolatility(closes, 365);
            drawdown1y = maxDrawdown(bars, lastDate.minusYears(1));

            sma50 = sma(closes, 50);
            sma200 = sma(closes, 200);
            if (sma50 != null && sma50 > 0) vsSma50 = round2((current - sma50) / sma50 * 100.0);
            if (sma200 != null && sma200 > 0) vsSma200 = round2((current - sma200) / sma200 * 100.0);
        }

        return new CryptoAnalysisDTO(
                q.getCoin(), q.getCoinName(), q.getImageUrl(), q.getCurrency(),
                q.getPrice(), priceUsd, q.getUsdToBrlRate(),
                q.getChangeValue(), q.getChangePercent(), q.getDayHigh(), q.getDayLow(),
                q.getVolume(), q.getMarketTime(), q.getSyncedAt(),
                volumeRank, totalCoins,
                high52w, low52w, position52w,
                athPrice, athDate, distFromAth != null ? round2(distFromAth) : null,
                returns,
                vol30, vol1y, drawdown1y,
                sma50, sma200, vsSma50, vsSma200,
                historyDays, historyStart
        );
    }

    /** Return (%) from the close on/before the target date to the current price. */
    private static void putReturn(Map<String, Double> returns, String key,
                                  List<PricePoint> bars, double current, LocalDate target) {
        if (bars.get(0).date().isAfter(target)) return; // not enough history for this period
        BigDecimal base = null;
        for (PricePoint p : bars) {
            if (p.date().isAfter(target)) break;
            base = p.close();
        }
        if (base != null && base.doubleValue() > 0) {
            returns.put(key, round2((current - base.doubleValue()) / base.doubleValue() * 100.0));
        }
    }

    /** Annualized standard deviation (%) of daily log returns over the last {@code window} bars. */
    private static Double annualizedVolatility(double[] closes, int window) {
        int n = closes.length;
        int start = Math.max(1, n - window);
        int count = n - start;
        if (count < 5) return null; // too few samples to be meaningful

        double[] logReturns = new double[count];
        int idx = 0;
        for (int i = start; i < n; i++) {
            if (closes[i - 1] <= 0 || closes[i] <= 0) return null;
            logReturns[idx++] = Math.log(closes[i] / closes[i - 1]);
        }
        double mean = 0;
        for (double r : logReturns) mean += r;
        mean /= count;
        double var = 0;
        for (double r : logReturns) var += (r - mean) * (r - mean);
        var /= (count - 1);
        return round2(Math.sqrt(var) * Math.sqrt(PERIODS_PER_YEAR) * 100.0);
    }

    /** Worst peak-to-trough decline (%) on closes since {@code from}; negative value. */
    private static Double maxDrawdown(List<PricePoint> bars, LocalDate from) {
        double peak = 0, worst = 0;
        int samples = 0;
        for (PricePoint p : bars) {
            if (p.date().isBefore(from)) continue;
            double c = p.close().doubleValue();
            samples++;
            if (c > peak) peak = c;
            else if (peak > 0) worst = Math.min(worst, (c - peak) / peak * 100.0);
        }
        return samples >= 5 ? round2(worst) : null;
    }

    private static Double sma(double[] closes, int window) {
        if (closes.length < window) return null;
        double sum = 0;
        for (int i = closes.length - window; i < closes.length; i++) sum += closes[i];
        return sum / window;
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(100.0, v));
    }

    private static Double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
