package tech.lougon.profitly.stock.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class StockQuote {

    private final String requestedSymbol;
    private final String symbol;
    private final Boolean changed;

    private final StockQuoteData data;

    private final Instant requestedAt;
    private final Long took;
}