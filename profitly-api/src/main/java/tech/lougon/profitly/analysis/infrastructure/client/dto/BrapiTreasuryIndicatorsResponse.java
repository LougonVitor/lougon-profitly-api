package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiTreasuryIndicatorsResponse(List<TreasuryIndicator> treasuries) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TreasuryIndicator(
            String symbol,
            Double buyRate,
            Double sellRate,
            Double buyPrice,
            Double sellPrice,
            Double basePrice,
            Integer duration
    ) {}
}
