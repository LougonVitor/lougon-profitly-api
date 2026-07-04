package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiTreasuryHistoryResponse(List<TreasuryHistoryEntry> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TreasuryHistoryEntry(
            String symbol,
            String referenceDate,
            Double buyRate,
            Double sellRate,
            Double buyPrice,
            Double sellPrice,
            Double basePrice
    ) {}
}
