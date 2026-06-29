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
        StockQuoteJpaEntity entity = quoteMapper.toEntity(stockQuote);
        StockQuoteJpaEntity saved = jpaStockRepository.save(entity);
        return quoteMapper.toDomain(saved);
    }

    @Override
    public boolean existsByTicker(String ticker) {
        return jpaStockRepository.existsBySymbol(ticker);
    }
}