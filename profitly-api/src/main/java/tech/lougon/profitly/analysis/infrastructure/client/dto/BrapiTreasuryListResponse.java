package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiTreasuryListResponse(List<TreasuryItem> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TreasuryItem(
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
            BrapiTreasuryRateInfo rateInfo
    ) {}
}
