package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Response of /api/v2/funds/indicators — root key is "funds". */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiFundIndicatorsResponse(List<FundIndicators> funds) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FundIndicators(
            String symbol,
            String cnpj,
            String name,
            String assetType,
            String asOfDate,
            Double price,
            Double navPerShare,
            Double priceToNav,
            Double equity,
            Double totalAssets,
            Number totalInvestors,
            Double dailyApplications,
            Double dailyRedemptions,
            Double sharesOutstanding,
            Double monthlyReturn,
            Double patrimonialMonthlyReturn,
            Double dividendYieldMonthly
    ) {}
}
