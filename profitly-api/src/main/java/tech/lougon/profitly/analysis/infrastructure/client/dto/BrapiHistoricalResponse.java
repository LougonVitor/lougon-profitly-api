package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiHistoricalResponse(
        @JsonProperty("results") List<Result> results
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            @JsonProperty("symbol") String symbol,
            @JsonProperty("data") Data data
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            @JsonProperty("historicalDataPrice") List<PriceBar> historicalDataPrice
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PriceBar(
            @JsonProperty("date")          Long date,
            @JsonProperty("open")          Double open,
            @JsonProperty("high")          Double high,
            @JsonProperty("low")           Double low,
            @JsonProperty("close")         Double close,
            @JsonProperty("volume")        Long volume,
            @JsonProperty("adjustedClose") Double adjustedClose
    ) {}
}
