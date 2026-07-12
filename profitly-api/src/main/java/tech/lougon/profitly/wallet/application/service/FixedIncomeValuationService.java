package tech.lougon.profitly.wallet.application.service;

import org.springframework.stereotype.Service;
import tech.lougon.profitly.wallet.domain.model.Indexer;
import tech.lougon.profitly.wallet.domain.port.MacroIndexLookup;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Computes the accrued value factor of a renda-fixa position (CDB/LCI/LCA/LC/LF/RDB):
 * principal × factor = current value. A position is modeled as a "cota" born at 1.0 that
 * grows day by day — this mirrors a market "price" so the rest of the wallet module
 * (average cost, P/L, evolution) can treat it exactly like a ticker's currentPrice.
 *
 * <p>These formulas are a deliberate MVP approximation, not the official ANBIMA/NTN-B
 * day-count convention (252 úteis): CDI/Selic compound over whatever days actually have a
 * published observation (weekends/holidays/publication lag are silently skipped, never
 * extrapolated past the last available value); Prefixado and the IPCA+ spread compound
 * over 365 calendar days.
 */
@Service
public class FixedIncomeValuationService {

    private static final int SCALE = 12;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final MacroIndexLookup macroIndexLookup;

    public FixedIncomeValuationService(MacroIndexLookup macroIndexLookup) {
        this.macroIndexLookup = macroIndexLookup;
    }

    /** Accrued factor at {@code asOf}, capped at {@code maturityDate} when it has already passed. */
    public BigDecimal factorAt(Indexer indexer, BigDecimal ratePercent, LocalDate start, LocalDate asOf, LocalDate maturityDate) {
        LocalDate end = asOf;
        if (maturityDate != null && maturityDate.isBefore(end)) end = maturityDate;
        if (!end.isAfter(start)) return ONE;

        return switch (indexer) {
            case CDI -> dailyIndexFactor("cdi", ratePercent, start, end);
            case SELIC -> selicFactor(ratePercent, start, end);
            case IPCA -> ipcaFactor(ratePercent, start, end);
            case PREFIXADO -> compoundOverCalendarDays(ratePercent, start, end);
        };
    }

    /** Sampled factor series (one point per {@code sampleDates} entry) for the wallet evolution chart. */
    public NavigableMap<LocalDate, BigDecimal> factorSeries(Indexer indexer, BigDecimal ratePercent, LocalDate start,
                                                              Iterable<LocalDate> sampleDates, LocalDate maturityDate) {
        NavigableMap<LocalDate, BigDecimal> series = new TreeMap<>();
        for (LocalDate date : sampleDates) {
            series.put(date, factorAt(indexer, ratePercent, start, date, maturityDate));
        }
        return series;
    }

    /** CDI (percentPerDay, already the daily rate — just apply ratePercent% of it). */
    private BigDecimal dailyIndexFactor(String slug, BigDecimal ratePercent, LocalDate start, LocalDate end) {
        BigDecimal multiplier = ratePercent.divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
        BigDecimal factor = ONE;
        for (var point : macroIndexLookup.findObservations(slug, start.plusDays(1), end)) {
            if (point.value() == null) continue;
            BigDecimal dailyFraction = point.value().divide(HUNDRED, SCALE, RoundingMode.HALF_UP);
            factor = round(factor.multiply(ONE.add(dailyFraction.multiply(multiplier))));
        }
        return factor;
    }

    /** Selic is published as an annual rate (percentPerYear) — convert to its daily-compounding equivalent first. */
    private BigDecimal selicFactor(BigDecimal ratePercent, LocalDate start, LocalDate end) {
        double multiplier = ratePercent.doubleValue() / 100.0;
        BigDecimal factor = ONE;
        for (var point : macroIndexLookup.findObservations("selic", start.plusDays(1), end)) {
            if (point.value() == null) continue;
            double dailyRate = Math.pow(1 + point.value().doubleValue() / 100.0, 1.0 / 252.0) - 1.0;
            factor = round(factor.multiply(ONE.add(BigDecimal.valueOf(dailyRate * multiplier))));
        }
        return factor;
    }

    /**
     * IPCA+ hybrid: monthly IPCA compounding (pro-rated on the boundary months where the
     * position doesn't cover the whole month) times a fixed annual spread compounded over
     * calendar days — an approximation of the NTN-B-style pro-rata, not the official one.
     */
    private BigDecimal ipcaFactor(BigDecimal ratePercent, LocalDate start, LocalDate end) {
        BigDecimal ipcaFactor = ONE;
        for (var point : macroIndexLookup.findObservations("ipca", start.withDayOfMonth(1), end)) {
            if (point.value() == null) continue;
            LocalDate monthStart = point.date().withDayOfMonth(1);
            LocalDate monthEndExclusive = monthStart.plusMonths(1);
            LocalDate rangeStart = monthStart.isBefore(start) ? start : monthStart;
            LocalDate rangeEndExclusive = monthEndExclusive.isAfter(end) ? end : monthEndExclusive;
            if (!rangeEndExclusive.isAfter(rangeStart)) continue;

            long daysInMonth = ChronoUnit.DAYS.between(monthStart, monthEndExclusive);
            long overlapDays = ChronoUnit.DAYS.between(rangeStart, rangeEndExclusive);
            double fraction = (double) overlapDays / (double) daysInMonth;
            double monthFactor = Math.pow(1 + point.value().doubleValue() / 100.0, fraction);
            ipcaFactor = round(ipcaFactor.multiply(BigDecimal.valueOf(monthFactor)));
        }
        return round(ipcaFactor.multiply(compoundOverCalendarDays(ratePercent, start, end)));
    }

    /** Prefixado (and the IPCA+ fixed spread): simple annual-rate compounding over calendar days / 365. */
    private BigDecimal compoundOverCalendarDays(BigDecimal ratePercent, LocalDate start, LocalDate end) {
        long days = ChronoUnit.DAYS.between(start, end);
        double factor = Math.pow(1 + ratePercent.doubleValue() / 100.0, days / 365.0);
        return round(BigDecimal.valueOf(factor));
    }

    private static BigDecimal round(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
