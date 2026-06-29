package tech.lougon.profitly.stock.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class StockQuote {

    public String requestedSymbol;
    public final String symbol;
    public final Boolean changed;

    public final StockQuoteData data;

    public final Instant requestedAt;
    public final Long took;
}