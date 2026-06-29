package tech.lougon.profitly.stock.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import tech.lougon.profitly.stock.domain.model.StockQuote;
import tech.lougon.profitly.stock.domain.model.StockQuoteData;
import tech.lougon.profitly.stock.infrastructure.persistence.StockQuoteJpaEntity;

@Component
public class InfraStockQuoteMapper {
    public StockQuote toDomain(StockQuoteJpaEntity entity) {
        StockQuoteData data = new StockQuoteData(
                entity.getShortName(),
                entity.getLongName(),
                entity.getCurrency(),
                entity.getRegularMarketPrice(),
                entity.getRegularMarketDayHigh(),
                entity.getRegularMarketDayLow(),
                entity.getRegularMarketDayRange(),
                entity.getRegularMarketChange(),
                entity.getRegularMarketChangePercent(),
                entity.getRegularMarketTime(),
                entity.getMarketCap(),
                entity.getRegularMarketVolume(),
                entity.getRegularMarketPreviousClose(),
                entity.getRegularMarketOpen(),
                entity.getFiftyTwoWeekRange(),
                entity.getFiftyTwoWeekLow(),
                entity.getFiftyTwoWeekHigh(),
                entity.getLogoUrl()
        );

        return new StockQuote(
                entity.getRequestedSymbol(),
                entity.getSymbol(),
                entity.getChanged(),
                data,
                entity.getRequestedAt(),
                entity.getTook()
        );
    }

    public StockQuoteJpaEntity toEntity(StockQuote stockQuote) {
        StockQuoteJpaEntity entity = new StockQuoteJpaEntity();
        entity.setRequestedSymbol(stockQuote.getRequestedSymbol());
        entity.setSymbol(stockQuote.getSymbol());
        entity.setChanged(stockQuote.getChanged());
        entity.setRequestedAt(stockQuote.getRequestedAt());
        entity.setTook(stockQuote.getTook());
        entity.setShortName(stockQuote.getData().shortName());
        entity.setLongName(stockQuote.getData().longName());
        entity.setCurrency(stockQuote.getData().currency());
        entity.setRegularMarketPrice(stockQuote.getData().regularMarketPrice());
        entity.setRegularMarketDayHigh(stockQuote.getData().regularMarketDayHigh());
        entity.setRegularMarketDayLow(stockQuote.getData().regularMarketDayLow());
        entity.setRegularMarketDayRange(stockQuote.getData().regularMarketDayRange());
        entity.setRegularMarketChange(stockQuote.getData().regularMarketChange());
        entity.setRegularMarketChangePercent(stockQuote.getData().regularMarketChangePercent());
        entity.setRegularMarketTime(stockQuote.getData().regularMarketTime());
        entity.setMarketCap(stockQuote.getData().marketCap());
        entity.setRegularMarketVolume(stockQuote.getData().regularMarketVolume());
        entity.setRegularMarketPreviousClose(stockQuote.getData().regularMarketPreviousClose());
        entity.setRegularMarketOpen(stockQuote.getData().regularMarketOpen());
        entity.setFiftyTwoWeekRange(stockQuote.getData().fiftyTwoWeekRange());
        entity.setFiftyTwoWeekLow(stockQuote.getData().fiftyTwoWeekLow());
        entity.setFiftyTwoWeekHigh(stockQuote.getData().fiftyTwoWeekHigh());
        entity.setLogoUrl(stockQuote.getData().logoUrl());
        return entity;
    }
}