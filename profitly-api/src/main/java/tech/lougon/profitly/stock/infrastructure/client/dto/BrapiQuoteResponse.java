package tech.lougon.profitly.stock.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiQuoteResponse(
        List<BrapiQuoteResult> results,
        Instant requestedAt,
        Long took
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BrapiQuoteResult(
            String requestedSymbol,
            String symbol,
            Boolean changed,
            BrapiQuoteData data
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BrapiQuoteData(
            String shortName,
            String longName,
            String currency,
            BigDecimal regularMarketPrice,
            BigDecimal regularMarketDayHigh,
            BigDecimal regularMarketDayLow,
            String regularMarketDayRange,
            BigDecimal regularMarketChange,
            BigDecimal regularMarketChangePercent,
            Instant regularMarketTime,
            Long marketCap,
            Long regularMarketVolume,
            BigDecimal regularMarketPreviousClose,
            BigDecimal regularMarketOpen,
            String fiftyTwoWeekRange,
            BigDecimal fiftyTwoWeekLow,
            BigDecimal fiftyTwoWeekHigh,
            @JsonProperty("logourl") String logoUrl
    ) {}
}