package tech.lougon.profitly.wallet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.lougon.profitly.wallet.application.service.FixedIncomeValuationService;
import tech.lougon.profitly.wallet.domain.model.Indexer;
import tech.lougon.profitly.wallet.domain.port.MacroIndexLookup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for the renda-fixa accrual math using an in-memory fake for the macro-index
 * series, so the four indexer branches (CDI, Selic, IPCA+, Prefixado) are exercised
 * without Spring or a database. Selic/IPCA/Prefixado expectations are derived from the
 * same underlying formula as the production code (Math.pow is deterministic) — the point
 * of these tests is to verify the *composition* (date ranges, month pro-rata, dispatch per
 * indexer), not to second-guess floating point exponentiation.
 */
class FixedIncomeValuationServiceTest {

    private FakeMacroIndexLookup macroIndexLookup;
    private FixedIncomeValuationService service;

    @BeforeEach
    void setUp() {
        macroIndexLookup = new FakeMacroIndexLookup();
        service = new FixedIncomeValuationService(macroIndexLookup);
    }

    @Test
    void cdiCompoundsDailyObservationsAtTheContractedPercentage() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        macroIndexLookup.add("cdi", LocalDate.of(2026, 1, 2), "0.05");
        macroIndexLookup.add("cdi", LocalDate.of(2026, 1, 3), "0.05");

        // 110% of CDI, two days at 0.05%/day each: (1 + 0.0005*1.10)^2
        BigDecimal factor = service.factorAt(Indexer.CDI, bd(110), start, LocalDate.of(2026, 1, 3), null);

        BigDecimal expected = BigDecimal.ONE.add(bd(0.0005).multiply(bd(1.10)))
                .multiply(BigDecimal.ONE.add(bd(0.0005).multiply(bd(1.10))));
        assertThat(factor.doubleValue()).isCloseTo(expected.doubleValue(), within(1e-9));
    }

    @Test
    void cdiCarriesTheLastKnownRateForwardOverUnpublishedDaysLikeWeekends() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        macroIndexLookup.add("cdi", LocalDate.of(2026, 1, 2), "0.05"); // e.g. Friday
        // Jan 3rd/4th have no observation — a weekend, or "today" before the rate is out yet

        BigDecimal factor = service.factorAt(Indexer.CDI, bd(100), start, LocalDate.of(2026, 1, 4), null);

        // the Jan-2 rate is carried forward and applied on the 3rd and 4th too: 3 days total
        BigDecimal dailyStep = BigDecimal.ONE.add(bd(0.0005));
        BigDecimal expected = dailyStep.multiply(dailyStep).multiply(dailyStep);
        assertThat(factor.doubleValue()).isCloseTo(expected.doubleValue(), within(1e-9));
    }

    @Test
    void cdiDoesNotAccrueOnTheApplicationDayItself() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        macroIndexLookup.add("cdi", start, "0.05"); // same-day observation must be ignored

        BigDecimal factor = service.factorAt(Indexer.CDI, bd(100), start, start, null);

        assertThat(factor.doubleValue()).isEqualTo(1.0);
    }

    @Test
    void selicConvertsTheAnnualRateToItsDailyCompoundingEquivalent() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 2);
        macroIndexLookup.add("selic", end, "12.0");

        BigDecimal factor = service.factorAt(Indexer.SELIC, bd(100), start, end, null);

        double dailyRate = Math.pow(1 + 12.0 / 100.0, 1.0 / 252.0) - 1.0;
        double expected = 1 + dailyRate; // 100% of Selic
        assertThat(factor.doubleValue()).isCloseTo(expected, within(1e-9));
    }

    @Test
    void prefixadoCompoundsTheContractedRateOverCalendarDaysOver365() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = start.plusDays(100);

        BigDecimal factor = service.factorAt(Indexer.PREFIXADO, bd(12), start, end, null);

        double expected = Math.pow(1.12, 100.0 / 365.0);
        assertThat(factor.doubleValue()).isCloseTo(expected, within(1e-9));
    }

    @Test
    void ipcaCompoundsFullMonthsAndAppliesTheFixedSpreadOverCalendarDays() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 3, 1); // covers Jan (31d) and Feb (28d, 2026 not a leap year) in full
        macroIndexLookup.add("ipca", LocalDate.of(2026, 1, 1), "0.5");
        macroIndexLookup.add("ipca", LocalDate.of(2026, 2, 1), "0.3");

        BigDecimal factor = service.factorAt(Indexer.IPCA, bd(6), start, end, null);

        double ipcaFactor = Math.pow(1.005, 1.0) * Math.pow(1.003, 1.0);
        double spreadFactor = Math.pow(1.06, 59.0 / 365.0); // Jan(31)+Feb(28) calendar days
        assertThat(factor.doubleValue()).isCloseTo(ipcaFactor * spreadFactor, within(1e-9));
    }

    @Test
    void ipcaProRatesTheBoundaryMonthWhenThePositionStartsMidMonth() {
        LocalDate start = LocalDate.of(2026, 1, 16); // 15 of January's 31 days remain (16th..31st inclusive)
        LocalDate end = LocalDate.of(2026, 2, 1);
        macroIndexLookup.add("ipca", LocalDate.of(2026, 1, 1), "1.0");

        BigDecimal factor = service.factorAt(Indexer.IPCA, BigDecimal.ZERO, start, end, null);

        double fraction = 16.0 / 31.0; // Jan 16 (inclusive) .. Feb 1 (exclusive) = 16 days out of 31
        double expected = Math.pow(1.01, fraction); // spread is 0%, only the pro-rated IPCA month applies
        assertThat(factor.doubleValue()).isCloseTo(expected, within(1e-9));
    }

    @Test
    void accrualStopsAtMaturityEvenWhenQueriedLater() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate maturity = LocalDate.of(2026, 1, 10);
        macroIndexLookup.add("cdi", LocalDate.of(2026, 1, 5), "0.05");
        macroIndexLookup.add("cdi", LocalDate.of(2026, 1, 20), "0.05"); // after maturity — must not accrue

        BigDecimal factorAtMaturity = service.factorAt(Indexer.CDI, bd(100), start, maturity, maturity);
        BigDecimal factorQueriedMuchLater = service.factorAt(Indexer.CDI, bd(100), start, LocalDate.of(2026, 6, 1), maturity);

        assertThat(factorQueriedMuchLater).isEqualByComparingTo(factorAtMaturity);
    }

    @Test
    void factorSeriesSamplesOnePointPerRequestedDate() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        macroIndexLookup.add("cdi", LocalDate.of(2026, 1, 2), "0.05");
        macroIndexLookup.add("cdi", LocalDate.of(2026, 1, 3), "0.05");

        var series = service.factorSeries(Indexer.CDI, bd(100), start,
                List.of(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 3)), null);

        assertThat(series).hasSize(2);
        assertThat(series.get(LocalDate.of(2026, 1, 3)).doubleValue())
                .isGreaterThan(series.get(LocalDate.of(2026, 1, 2)).doubleValue());
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v);
    }

    /** In-memory fake standing in for the local macro_index_values table. */
    private static class FakeMacroIndexLookup implements MacroIndexLookup {
        private final List<Entry> entries = new ArrayList<>();

        void add(String slug, LocalDate date, String value) {
            entries.add(new Entry(slug, date, new BigDecimal(value)));
        }

        @Override
        public List<IndexPoint> findObservations(String slug, LocalDate start, LocalDate end) {
            return entries.stream()
                    .filter(e -> e.slug.equals(slug) && !e.date.isBefore(start) && !e.date.isAfter(end))
                    .sorted((a, b) -> a.date.compareTo(b.date))
                    .map(e -> new IndexPoint(e.date, e.value))
                    .toList();
        }

        private record Entry(String slug, LocalDate date, BigDecimal value) {}
    }
}
