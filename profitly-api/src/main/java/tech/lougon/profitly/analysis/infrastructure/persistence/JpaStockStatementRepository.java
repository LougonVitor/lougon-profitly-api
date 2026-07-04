package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaStockStatementRepository extends JpaRepository<StockStatementJpaEntity, Long> {

    List<StockStatementJpaEntity> findBySymbolAndStatementTypeOrderByEndDateDesc(String symbol, String statementType);

    Optional<StockStatementJpaEntity> findBySymbolAndStatementTypeAndPeriodTypeAndEndDate(
            String symbol, String statementType, String periodType, String endDate);
}
