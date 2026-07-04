package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiFiiIndicatorsHistoryResponse(List<FiiHistoryEntry> history) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FiiHistoryEntry(
            String symbol,
            String referenceDate,
            Double price,
            Double navPerShare,
            Double priceToNav,
            Double dividendYield12m,
            Double dividendYield1m,
            Double monthlyReturn,
            Long totalInvestors,
            Long sharesOutstanding,
            Double equity,
            Double totalAssets,
            String segmentType
    ) {}
}
