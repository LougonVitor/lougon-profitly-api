package tech.lougon.profitly.analysis.application.service;

import org.springframework.stereotype.Service;
import tech.lougon.profitly.analysis.application.dto.TreasuryAnalysisDTO;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaTreasuryBondHistoryRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaTreasuryBondRepository;
import tech.lougon.profitly.analysis.infrastructure.persistence.TreasuryBondHistoryJpaEntity;
import tech.lougon.profitly.analysis.infrastructure.persistence.TreasuryBondJpaEntity;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Computes advanced treasury indicators from the daily rate/price series stored
 * in treasury_bond_history. Everything is served from the database — no brapi call.
 */
@Service
public class TreasuryAnalysisService {

    /** Treasury trades on business days only, so annualization uses 252 periods. */
    private static final double PERIODS_PER_YEAR = 252.0;

    private final JpaTreasuryBondRepository bondRepo;
    private final JpaTreasuryBondHistoryRepository historyRepo;

    public TreasuryAnalysisService(JpaTreasuryBondRepository bondRepo,
                                   JpaTreasuryBondHistoryRepository historyRepo) {
        this.bondRepo = bondRepo;
        this.historyRepo = historyRepo;
    }

    public Optional<TreasuryAnalysisDTO> getAnalysis(String symbol) {
        TreasuryBondJpaEntity bond = bondRepo.findById(symbol).orElse(null);
        if (bond == null) return Optional.empty();

        List<TreasuryBondHistoryJpaEntity> history =
                historyRepo.findBySymbolOrderByReferenceDateAsc(symbol);
        List<TreasuryBondJpaEntity> allBonds = bondRepo.findAllByOrderByBuyRateDesc();

        return Optional.of(build(bond, history, allBonds));
    }

    private TreasuryAnalysisDTO build(TreasuryBondJpaEntity bond,
                                      List<TreasuryBondHistoryJpaEntity> history,
                                      List<TreasuryBondJpaEntity> allBonds) {
        // maturity countdown
        Long daysToMaturity = null;
        Double yearsToMaturity = null;
        LocalDate maturity = parseDate(bond.getMaturityDate());
        if (maturity != null) {
            daysToMaturity = ChronoUnit.DAYS.between(LocalDate.now(), maturity);
            yearsToMaturity = round2(daysToMaturity / 365.25);
        }

        Double rateSpread = null;
        if (bond.getBuyRate() != null && bond.getSellRate() != null) {
            rateSpread = round2(bond.getBuyRate() - bond.getSellRate());
        }

        // rate ranking among bonds sharing the indexer (rate semantics differ across indexers)
        Integer rateRank = null, totalInIndexer = null;
        List<TreasuryAnalysisDTO.SimilarBond> similar = new ArrayList<>();
        if (bond.getIndexer() != null) {
            List<TreasuryBondJpaEntity> sameIndexer = allBonds.stream()
                    .filter(b -> bond.getIndexer().equals(b.getIndexer()))
                    .toList();
            totalInIndexer = sameIndexer.size();
            for (int i = 0; i < sameIndexer.size(); i++) {
                if (bond.getSymbol().equals(sameIndexer.get(i).getSymbol())) { rateRank = i + 1; break; }
            }
            sameIndexer.stream()
                    .filter(b -> !bond.getSymbol().equals(b.getSymbol()))
                    .sorted((a, b) -> {
                        String ma = a.getMaturityDate() != null ? a.getMaturityDate() : "";
                        String mb = b.getMaturityDate() != null ? b.getMaturityDate() : "";
                        return ma.compareTo(mb);
                    })
                    .forEach(b -> similar.add(new TreasuryAnalysisDTO.SimilarBond(
                            b.getSymbol(), b.getBondType(), b.getCouponType(), b.getMaturityDate(),
                            b.getBuyRate(), b.getSellRate(), b.getBuyPrice())));
        }

        // history-driven indicators
        List<Bar> bars = toBars(history);
        int n = bars.size();

        Map<String, Double> rateChanges = new LinkedHashMap<>();
        Map<String, Double> priceReturns = new LinkedHashMap<>();
        Double rate52wHigh = null, rate52wLow = null, ratePosition = null;
        Double rateHigh = null, rateLow = null;
        String rateHighDate = null, rateLowDate = null;
        Double vol1y = null, drawdown1y = null;
        Integer historyDays = n > 0 ? n : null;
        String historyStart = n > 0 ? bars.get(0).date().toString() : null;

        if (n > 0) {
            LocalDate lastDate = bars.get(n - 1).date();
            double currentRate = bond.getBuyRate() != null ? bond.getBuyRate() : bars.get(n - 1).rate();
            Double currentPrice = bond.getSellPrice() != null ? bond.getSellPrice() : bars.get(n - 1).price();

            putRateChange(rateChanges, "1m", bars, currentRate, lastDate.minusMonths(1));
            putRateChange(rateChanges, "3m", bars, currentRate, lastDate.minusMonths(3));
            putRateChange(rateChanges, "6m", bars, currentRate, lastDate.minusMonths(6));
            putRateChange(rateChanges, "1y", bars, currentRate, lastDate.minusYears(1));
            rateChanges.put("max", round2(currentRate - bars.get(0).rate()));

            if (currentPrice != null) {
                putPriceReturn(priceReturns, "1m", bars, currentPrice, lastDate.minusMonths(1));
                putPriceReturn(priceReturns, "3m", bars, currentPrice, lastDate.minusMonths(3));
                putPriceReturn(priceReturns, "6m", bars, currentPrice, lastDate.minusMonths(6));
                putPriceReturn(priceReturns, "1y", bars, currentPrice, lastDate.minusYears(1));
                putPriceReturn(priceReturns, "max", bars, currentPrice, LocalDate.MIN);
            }

            LocalDate cutoff52 = lastDate.minusWeeks(52);
            double hi = -Double.MAX_VALUE, lo = Double.MAX_VALUE;
            for (Bar b : bars) {
                if (b.date().isBefore(cutoff52)) continue;
                if (b.rate() > hi) hi = b.rate();
                if (b.rate() < lo) lo = b.rate();
            }
            if (hi > -Double.MAX_VALUE) {
                rate52wHigh = hi;
                rate52wLow = lo;
                if (hi > lo) ratePosition = clamp((currentRate - lo) / (hi - lo) * 100.0);
            }

            for (Bar b : bars) {
                if (rateHigh == null || b.rate() > rateHigh) { rateHigh = b.rate(); rateHighDate = b.date().toString(); }
                if (rateLow == null || b.rate() < rateLow) { rateLow = b.rate(); rateLowDate = b.date().toString(); }
            }

            vol1y = annualizedVolatility(bars, lastDate.minusYears(1));
            drawdown1y = maxDrawdown(bars, lastDate.minusYears(1));
        }

        return new TreasuryAnalysisDTO(
                bond.getSymbol(), bond.getBondType(), bond.getIndexer(), bond.getCouponType(),
                bond.getMaturityDate(), bond.getDurationDays(), bond.getBaseDate(),
                bond.getBuyRate(), bond.getSellRate(), bond.getBuyPrice(), bond.getSellPrice(), bond.getBasePrice(),
                bond.getRateType(), bond.getRateUnit(), bond.getRateDescription(),
                bond.getSyncedAt(),
                daysToMaturity, yearsToMaturity,
                rateSpread, rateChanges,
                rate52wHigh, rate52wLow, ratePosition,
                rateHigh, rateHighDate, rateLow, rateLowDate,
                priceReturns, vol1y, drawdown1y,
                rateRank, totalInIndexer,
                historyDays, historyStart,
                similar
        );
    }

    /** One usable history sample: buyRate plus the mark-to-market price (sellPrice, basePrice fallback). */
    private record Bar(LocalDate date, double rate, Double price) {}

    private static List<Bar> toBars(List<TreasuryBondHistoryJpaEntity> history) {
        List<Bar> bars = new ArrayList<>(history.size());
        for (TreasuryBondHistoryJpaEntity h : history) {
            LocalDate date = parseDate(h.getReferenceDate());
            if (date == null || h.getBuyRate() == null) continue;
            Double price = h.getSellPrice() != null ? h.getSellPrice() : h.getBasePrice();
            bars.add(new Bar(date, h.getBuyRate(), price));
        }
        return bars;
    }

    /** Rate delta (p.p.) from the sample on/before the target date to the current rate. */
    private static void putRateChange(Map<String, Double> changes, String key,
                                      List<Bar> bars, double currentRate, LocalDate target) {
        if (bars.get(0).date().isAfter(target)) return; // not enough history for this period
        Double base = null;
        for (Bar b : bars) {
            if (b.date().isAfter(target)) break;
            base = b.rate();
        }
        if (base != null) changes.put(key, round2(currentRate - base));
    }

    /** Price return (%) from the sample on/before the target date to the current price. */
    private static void putPriceReturn(Map<String, Double> returns, String key,
                                       List<Bar> bars, double currentPrice, LocalDate target) {
        boolean isMax = LocalDate.MIN.equals(target);
        if (!isMax && bars.get(0).date().isAfter(target)) return;
        Double base = null;
        for (Bar b : bars) {
            if (!isMax && b.date().isAfter(target)) break;
            if (b.price() != null) { base = b.price(); if (isMax) break; }
        }
        if (base != null && base > 0) {
            returns.put(key, round2((currentPrice - base) / base * 100.0));
        }
    }

    /** Annualized standard deviation (%) of daily price log returns since {@code from}. */
    private static Double annualizedVolatility(List<Bar> bars, LocalDate from) {
        List<Double> prices = new ArrayList<>();
        for (Bar b : bars) {
            if (b.date().isBefore(from) || b.price() == null || b.price() <= 0) continue;
            prices.add(b.price());
        }
        int count = prices.size() - 1;
        if (count < 5) return null; // too few samples to be meaningful

        double mean = 0;
        double[] logReturns = new double[count];
        for (int i = 1; i < prices.size(); i++) {
            logReturns[i - 1] = Math.log(prices.get(i) / prices.get(i - 1));
            mean += logReturns[i - 1];
        }
        mean /= count;
        double var = 0;
        for (double r : logReturns) var += (r - mean) * (r - mean);
        var /= (count - 1);
        return round2(Math.sqrt(var) * Math.sqrt(PERIODS_PER_YEAR) * 100.0);
    }

    /** Worst peak-to-trough decline (%) of the price since {@code from}; negative value. */
    private static Double maxDrawdown(List<Bar> bars, LocalDate from) {
        double peak = 0, worst = 0;
        int samples = 0;
        for (Bar b : bars) {
            if (b.date().isBefore(from) || b.price() == null) continue;
            samples++;
            if (b.price() > peak) peak = b.price();
            else if (peak > 0) worst = Math.min(worst, (b.price() - peak) / peak * 100.0);
        }
        return samples >= 5 ? round2(worst) : null;
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.length() < 10) return null;
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(100.0, v));
    }

    private static Double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
