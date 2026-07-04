package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaStockQuoteRepository extends JpaRepository<StockQuoteJpaEntity, String> {
}
