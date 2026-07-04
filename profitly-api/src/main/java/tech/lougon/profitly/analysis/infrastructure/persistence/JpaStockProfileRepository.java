package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaStockProfileRepository extends JpaRepository<StockProfileJpaEntity, String> {
    List<StockProfileJpaEntity> findBySector(String sector);
}
