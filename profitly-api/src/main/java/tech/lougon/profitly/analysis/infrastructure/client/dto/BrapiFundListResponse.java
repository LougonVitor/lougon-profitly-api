package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiFundListResponse(List<FundItem> funds) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FundItem(
            String symbol,
            String name,
            String type,
            Double price,
            Double dividendYield12m,
            Double dividendYield1m,
            Double priceToNav,
            Double navPerShare,
            Number totalInvestors,
            String administratorName,
            String administratorCnpj,
            String segmentType
    ) {}
}
