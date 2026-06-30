package tech.lougon.profitly.ticker.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import tech.lougon.profitly.ticker.domain.model.Ticker;
import tech.lougon.profitly.ticker.domain.repository.TickerRepository;
import tech.lougon.profitly.ticker.infrastructure.persistence.mapper.InfraTickerMapper;

import java.util.List;
import java.util.Optional;

@Repository
public class TickerRepositoryImpl implements TickerRepository {

    private final JpaTickerRepository jpa;
    private final InfraTickerMapper mapper;

    public TickerRepositoryImpl(JpaTickerRepository jpa, InfraTickerMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Ticker save(Ticker ticker) {
        TickerJpaEntity entity = jpa.findBySymbol(ticker.symbol())
                .map(existing -> { mapper.applyFields(existing, ticker); return existing; })
                .orElseGet(() -> mapper.toEntity(ticker));
        return mapper.toDomain(jpa.save(entity));
    }

    @Override
    public Optional<Ticker> findBySymbol(String symbol) {
        return jpa.findBySymbol(symbol).map(mapper::toDomain);
    }

    @Override
    public List<Ticker> findAll() {
        return jpa.findAll().stream().map(mapper::toDomain).toList();
    }
}
