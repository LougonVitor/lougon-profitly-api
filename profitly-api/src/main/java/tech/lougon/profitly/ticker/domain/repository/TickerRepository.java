package tech.lougon.profitly.ticker.domain.repository;

import tech.lougon.profitly.ticker.domain.model.Ticker;

import java.util.List;
import java.util.Optional;

public interface TickerRepository {
    Ticker save(Ticker ticker);
    Optional<Ticker> findBySymbol(String symbol);
    List<Ticker> findAll();
}
