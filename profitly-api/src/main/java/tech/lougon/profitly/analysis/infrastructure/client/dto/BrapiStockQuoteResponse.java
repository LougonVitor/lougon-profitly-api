package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Response of /api/v2/stocks/quote?symbols=... */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiStockQuoteResponse(List<Result> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(String symbol, Data data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            String shortName,
            String longName,
            String currency,
            Double regularMarketPrice,
            Double regularMarketDayHigh,
            Double regularMarketDayLow,
            Double regularMarketChange,
            Double regularMarketChangePercent,
            String regularMarketTime,
            Long marketCap,
            Long regularMarketVolume,
            Double regularMarketPreviousClose,
            Double regularMarketOpen,
            Double fiftyTwoWeekLow,
            Double fiftyTwoWeekHigh,
            @JsonProperty("logourl") String logoUrl
    ) {}
}
