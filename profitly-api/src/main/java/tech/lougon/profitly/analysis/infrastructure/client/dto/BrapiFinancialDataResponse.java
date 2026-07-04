package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiFinancialDataResponse(
        @JsonProperty("results") List<Result> results
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            @JsonProperty("symbol") String symbol,
            @JsonProperty("data") Data data
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            @JsonProperty("currentPrice")          BigDecimal currentPrice,
            @JsonProperty("revenuePerShare")       BigDecimal revenuePerShare,
            @JsonProperty("returnOnAssets")        BigDecimal returnOnAssets,
            @JsonProperty("returnOnEquity")        BigDecimal returnOnEquity,
            @JsonProperty("grossMargins")          BigDecimal grossMargins,
            @JsonProperty("operatingMargins")      BigDecimal operatingMargins,
            @JsonProperty("profitMargins")         BigDecimal profitMargins,
            @JsonProperty("revenueGrowth")         BigDecimal revenueGrowth,
            @JsonProperty("earningsGrowth")        BigDecimal earningsGrowth,
            @JsonProperty("currentRatio")          BigDecimal currentRatio,
            @JsonProperty("quickRatio")            BigDecimal quickRatio,
            @JsonProperty("debtToEquity")          BigDecimal debtToEquity,
            @JsonProperty("totalCash")             Long totalCash,
            @JsonProperty("totalDebt")             Long totalDebt,
            @JsonProperty("totalRevenue")          Long totalRevenue,
            @JsonProperty("grossProfits")          Long grossProfits,
            @JsonProperty("ebitda")                Long ebitda,
            @JsonProperty("freeCashflow")          Long freeCashflow,
            @JsonProperty("operatingCashflow")     Long operatingCashflow,
            @JsonProperty("totalCashPerShare")     BigDecimal totalCashPerShare,
            @JsonProperty("ebitdaMargins")         BigDecimal ebitdaMargins,
            @JsonProperty("earningsGrowthAnnual")  BigDecimal earningsGrowthAnnual,
            @JsonProperty("revenueGrowthAnnual")   BigDecimal revenueGrowthAnnual
    ) {}
}
