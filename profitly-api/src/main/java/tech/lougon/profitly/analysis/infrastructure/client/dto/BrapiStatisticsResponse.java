package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiStatisticsResponse(
        @JsonProperty("results") List<Result> results
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            @JsonProperty("symbol") String symbol,
            @JsonProperty("data") Data data
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            @JsonProperty("trailingPE")           BigDecimal trailingPE,
            @JsonProperty("forwardPE")            BigDecimal forwardPE,
            @JsonProperty("priceToBook")          BigDecimal priceToBook,
            @JsonProperty("dividendYield")        BigDecimal dividendYield,
            @JsonProperty("beta")                 BigDecimal beta,
            @JsonProperty("earningsPerShare")     BigDecimal earningsPerShare,
            @JsonProperty("trailingEps")          BigDecimal trailingEps,
            @JsonProperty("pegRatio")             BigDecimal pegRatio,
            @JsonProperty("enterpriseToRevenue")  BigDecimal enterpriseToRevenue,
            @JsonProperty("enterpriseToEbitda")   BigDecimal enterpriseToEbitda,
            @JsonProperty("marketCap")            Long marketCap,
            @JsonProperty("enterpriseValue")      Long enterpriseValue,
            @JsonProperty("bookValue")            BigDecimal bookValue,
            @JsonProperty("52WeekChange")         BigDecimal weekChange52,
            @JsonProperty("profitMargins")        BigDecimal profitMargins,
            @JsonProperty("sharesOutstanding")    Long sharesOutstanding,
            @JsonProperty("floatShares")          Long floatShares,
            @JsonProperty("lastDividendValue")    BigDecimal lastDividendValue,
            @JsonProperty("lastDividendDate")     String lastDividendDate
    ) {}
}
