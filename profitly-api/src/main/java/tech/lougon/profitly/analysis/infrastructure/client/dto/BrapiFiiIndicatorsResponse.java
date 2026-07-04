package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiFiiIndicatorsResponse(List<FiiIndicatorWithInfo> fiis) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FiiIndicatorWithInfo(
            String symbol,
            FiiIndicator data,
            AdminInfo administrator
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FiiIndicator(
            String asOfDate,
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AdminInfo(
            String name,
            String cnpj
    ) {}
}
