package tech.lougon.profitly.ticker.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiTickerResponse(
        @JsonProperty("results") List<TickerItem> results,
        @JsonProperty("pagination") Pagination pagination
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TickerItem(
            @JsonProperty("symbol")    String symbol,
            @JsonProperty("name")      String name,
            @JsonProperty("longName")  String longName,
            @JsonProperty("assetType") String assetType,
            @JsonProperty("subType")   String subType,
            @JsonProperty("exchange")  String exchange,
            @JsonProperty("currency")  String currency,
            @JsonProperty("sector")    String sector,
            @JsonProperty("isActive")  Boolean isActive,
            @JsonProperty("logoUrl")   String logoUrl,
            @JsonProperty("quote")     QuoteSummary quote
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuoteSummary(
            @JsonProperty("lastPrice")     BigDecimal lastPrice,
            @JsonProperty("changePercent") BigDecimal changePercent,
            @JsonProperty("volume")        Long volume,
            @JsonProperty("marketCap")     Long marketCap
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pagination(
            @JsonProperty("hasNextPage") boolean hasNextPage
    ) {}
}
