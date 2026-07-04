package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaStockFinancialsRepository extends JpaRepository<StockFinancialsJpaEntity, String> {
    List<StockFinancialsJpaEntity> findBySymbolIn(List<String> symbols);
}
