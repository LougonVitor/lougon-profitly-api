package tech.lougon.profitly.stock.service.mapper;

import org.springframework.stereotype.Component;
import tech.lougon.profitly.stock.service.dto.StockQuoteDTO;
import tech.lougon.profitly.stock.domain.model.StockQuote;
import tech.lougon.profitly.stock.domain.model.StockQuoteData;
import tech.lougon.profitly.stock.infrastructure.client.dto.BrapiQuoteResponse;

@Component
public class StockQuoteMapper {

    public StockQuoteDTO toDTO(StockQuote stockQuote) {
        return new StockQuoteDTO(
                stockQuote.getRequestedSymbol(),
                stockQuote.getSymbol(),
                stockQuote.getChanged(),
                stockQuote.getRequestedAt(),
                stockQuote.getTook(),
                stockQuote.getData().shortName(),
                stockQuote.getData().longName(),
                stockQuote.getData().currency(),
                stockQuote.getData().regularMarketPrice(),
                stockQuote.getData().regularMarketDayHigh(),
                stockQuote.getData().regularMarketDayLow(),
                stockQuote.getData().regularMarketDayRange(),
                stockQuote.getData().regularMarketChange(),
                stockQuote.getData().regularMarketChangePercent(),
                stockQuote.getData().regularMarketTime(),
                stockQuote.getData().marketCap(),
                stockQuote.getData().regularMarketVolume(),
                stockQuote.getData().regularMarketPreviousClose(),
                stockQuote.getData().regularMarketOpen(),
                stockQuote.getData().fiftyTwoWeekRange(),
                stockQuote.getData().fiftyTwoWeekLow(),
                stockQuote.getData().fiftyTwoWeekHigh(),
                stockQuote.getData().logoUrl()
        );
    }

    public StockQuote toDomain(BrapiQuoteResponse.BrapiQuoteResult result, BrapiQuoteResponse response) {
        BrapiQuoteResponse.BrapiQuoteData d = result.data();

        StockQuoteData data = new StockQuoteData(
                d.shortName(),
                d.longName(),
                d.currency(),
                d.regularMarketPrice(),
                d.regularMarketDayHigh(),
                d.regularMarketDayLow(),
                d.regularMarketDayRange(),
                d.regularMarketChange(),
                d.regularMarketChangePercent(),
                d.regularMarketTime(),
                d.marketCap(),
                d.regularMarketVolume(),
                d.regularMarketPreviousClose(),
                d.regularMarketOpen(),
                d.fiftyTwoWeekRange(),
                d.fiftyTwoWeekLow(),
                d.fiftyTwoWeekHigh(),
                d.logoUrl()
        );

        return new StockQuote(
                result.requestedSymbol(),
                result.symbol(),
                result.changed(),
                data,
                response.requestedAt(),
                response.took()
        );
    }
}