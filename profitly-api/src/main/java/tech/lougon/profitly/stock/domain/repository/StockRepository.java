package tech.lougon.profitly.stock.domain.repository;

import tech.lougon.profitly.stock.domain.model.StockQuote;

import java.util.List;
import java.util.Optional;

public interface StockRepository {

    Optional<StockQuote> findByTicker(String ticker);

    List<StockQuote> findAll();

    StockQuote save(StockQuote stockQuote);

    boolean existsByTicker(String ticker);
}