package tech.lougon.profitly.stock.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record StockQuoteData(
        String shortName
        , String longName
        , String currency
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
        , String logoUrl
) {}