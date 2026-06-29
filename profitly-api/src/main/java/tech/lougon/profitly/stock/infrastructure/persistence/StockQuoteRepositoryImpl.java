package tech.lougon.profitly.stock.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import tech.lougon.profitly.stock.domain.model.StockQuote;
import tech.lougon.profitly.stock.domain.repository.StockRepository;
import tech.lougon.profitly.stock.infrastructure.persistence.mapper.InfraStockQuoteMapper;

import java.util.List;
import java.util.Optional;

@Repository
public class StockQuoteRepositoryImpl implements StockRepository {

    private final JpaStockQuoteRepository jpaStockRepository;
    private final InfraStockQuoteMapper quoteMapper;

    public StockQuoteRepositoryImpl(JpaStockQuoteRepository jpaStockRepository, InfraStockQuoteMapper stockQuoteMapper) {
        this.jpaStockRepository = jpaStockRepository;
        this.quoteMapper = stockQuoteMapper;
    }

    @Override
    public Optional<StockQuote> findByTicker(String ticker) {
        return jpaStockRepository.findBySymbol(ticker)
                .map(quoteMapper::toDomain);
    }

    @Override
    public List<StockQuote> findAll() {
        return jpaStockRepository.findAll()
                .stream()
                .map(quoteMapper::toDomain)
                .toList();
    }

    @Override
    public StockQuote save(StockQuote stockQuote) {
        StockQuoteJpaEntity entity = jpaStockRepository
                .findBySymbol(stockQuote.getSymbol())
                .map(existing -> updateEntity(existing, stockQuote))
                .orElseGet(() -> quoteMapper.toEntity(stockQuote));

        return quoteMapper.toDomain(jpaStockRepository.save(entity));
    }

    @Override
    public boolean existsByTicker(String ticker) {
        return jpaStockRepository.existsBySymbol(ticker);
    }

    private StockQuoteJpaEntity updateEntity(StockQuoteJpaEntity existing, StockQuote stockQuote) {
        existing.setRequestedSymbol(stockQuote.getRequestedSymbol());
        existing.setChanged(stockQuote.getChanged());
        existing.setRequestedAt(stockQuote.getRequestedAt());
        existing.setTook(stockQuote.getTook());
        existing.setShortName(stockQuote.getData().shortName());
        existing.setLongName(stockQuote.getData().longName());
        existing.setCurrency(stockQuote.getData().currency());
        existing.setRegularMarketPrice(stockQuote.getData().regularMarketPrice());
        existing.setRegularMarketDayHigh(stockQuote.getData().regularMarketDayHigh());
        existing.setRegularMarketDayLow(stockQuote.getData().regularMarketDayLow());
        existing.setRegularMarketDayRange(stockQuote.getData().regularMarketDayRange());
        existing.setRegularMarketChange(stockQuote.getData().regularMarketChange());
        existing.setRegularMarketChangePercent(stockQuote.getData().regularMarketChangePercent());
        existing.setRegularMarketTime(stockQuote.getData().regularMarketTime());
        existing.setMarketCap(stockQuote.getData().marketCap());
        existing.setRegularMarketVolume(stockQuote.getData().regularMarketVolume());
        existing.setRegularMarketPreviousClose(stockQuote.getData().regularMarketPreviousClose());
        existing.setRegularMarketOpen(stockQuote.getData().regularMarketOpen());
        existing.setFiftyTwoWeekRange(stockQuote.getData().fiftyTwoWeekRange());
        existing.setFiftyTwoWeekLow(stockQuote.getData().fiftyTwoWeekLow());
        existing.setFiftyTwoWeekHigh(stockQuote.getData().fiftyTwoWeekHigh());
        existing.setLogoUrl(stockQuote.getData().logoUrl());
        return existing;
    }
}