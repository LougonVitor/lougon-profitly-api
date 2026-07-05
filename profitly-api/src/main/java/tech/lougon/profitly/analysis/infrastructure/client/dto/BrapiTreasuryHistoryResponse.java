package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * /api/v2/treasury/indicators/history — one result per requested symbol (max 20),
 * each carrying its own nested daily series keyed by baseDate.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiTreasuryHistoryResponse(List<TreasuryHistoryResult> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TreasuryHistoryResult(
            String symbol,
            String bondType,
            String indexer,
            String couponType,
            String maturityDate,
            BrapiTreasuryRateInfo rateInfo,
            List<TreasuryHistoryEntry> history
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TreasuryHistoryEntry(
            String baseDate,
            Double buyRate,
            Double sellRate,
            Double buyPrice,
            Double sellPrice,
            Double basePrice
    ) {}
}
