package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaStockSplitEventRepository extends JpaRepository<StockSplitEventJpaEntity, Long> {
    List<StockSplitEventJpaEntity> findBySymbol(String symbol);
    Optional<StockSplitEventJpaEntity> findBySymbolAndLastDatePriorAndLabel(String symbol, String lastDatePrior, String label);
}
