package tech.lougon.profitly.stock.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaStockQuoteRepository extends JpaRepository<StockQuoteJpaEntity, String> {

    Optional<StockQuoteJpaEntity> findBySymbol(String symbol);

    boolean existsBySymbol(String symbol);
}