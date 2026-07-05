package tech.lougon.profitly.analysis.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Advanced treasury bond analysis: current snapshot enriched with indicators
 * computed from the daily rate/price history stored in treasury_bond_history.
 */
public record TreasuryAnalysisDTO(
        String symbol,
        String bondType,
        String indexer,
        String couponType,
        String maturityDate,
        Integer durationDays,
        String baseDate,
        Double buyRate,
        Double sellRate,
        Double buyPrice,
        Double sellPrice,
        Double basePrice,
        String rateType,
        String rateUnit,
        String rateDescription,
        Instant syncedAt,
        Long daysToMaturity,
        Double yearsToMaturity,
        /** buyRate − sellRate, in percentage points. */
        Double rateSpread,
        /** buyRate change in percentage points keyed by period: 1m, 3m, 6m, 1y, max. */
        Map<String, Double> rateChanges,
        Double rate52wHigh,
        Double rate52wLow,
        /** Where the current buyRate sits inside the 52-week range, 0–100. */
        Double ratePositionInRange52w,
        Double rateHistoryHigh,
        String rateHistoryHighDate,
        Double rateHistoryLow,
        String rateHistoryLowDate,
        /** Percent returns of the mark-to-market price (sellPrice) keyed by period: 1m, 3m, 6m, 1y, max. */
        Map<String, Double> priceReturns,
        /** Annualized volatility (%) of daily price log returns over the last year (252 sessions). */
        Double priceVolatility1y,
        /** Worst peak-to-trough decline (%) of the mark-to-market price in the last year, negative. */
        Double maxDrawdown1y,
        /** 1-based position of this bond's buyRate among bonds with the same indexer. */
        Integer rateRankInIndexer,
        Integer totalInIndexer,
        Integer historyDays,
        String historyStart,
        /** Other bonds with the same indexer, ordered by maturity. */
        List<SimilarBond> similarBonds
) {
    public record SimilarBond(
            String symbol,
            String bondType,
            String couponType,
            String maturityDate,
            Double buyRate,
            Double sellRate,
            Double buyPrice
    ) {}
}
