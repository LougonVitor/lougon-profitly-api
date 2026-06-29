package tech.lougon.profitly.stock.presentation.response;

import tech.lougon.profitly.stock.application.dto.StockQuoteDTO;

import java.math.BigDecimal;
import java.time.Instant;

public record StockQuoteResponse(
        String symbol
        , String shortName
        , String longName
        , String currency
        , String logoUrl
        , BigDecimal regularMarketPrice
        , BigDecimal regularMarketDayHigh
        , BigDecimal regularMarketDayLow
        , String regularMarketDayRange
        , BigDecimal regularMarketChange
        , BigDecimal regularMarketChangePercent
        , Instant regularMarketTime
        , Long marketCap
        , Long regularMarketVolume
        , BigDecimal regularMarketPreviousClose
        , BigDecimal regularMarketOpen
        , String fiftyTwoWeekRange
        , BigDecimal fiftyTwoWeekLow
        , BigDecimal fiftyTwoWeekHigh
) {
    public static StockQuoteResponse from(StockQuoteDTO dto) {
        return new StockQuoteResponse(
                dto.symbol(),
                dto.shortName(),
                dto.longName(),
                dto.currency(),
                dto.logoUrl(),
                dto.regularMarketPrice(),
                dto.regularMarketDayHigh(),
                dto.regularMarketDayLow(),
                dto.regularMarketDayRange(),
                dto.regularMarketChange(),
                dto.regularMarketChangePercent(),
                dto.regularMarketTime(),
                dto.marketCap(),
                dto.regularMarketVolume(),
                dto.regularMarketPreviousClose(),
                dto.regularMarketOpen(),
                dto.fiftyTwoWeekRange(),
                dto.fiftyTwoWeekLow(),
                dto.fiftyTwoWeekHigh()
        );
    }
}