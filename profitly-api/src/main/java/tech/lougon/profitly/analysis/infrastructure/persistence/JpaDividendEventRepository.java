package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaDividendEventRepository extends JpaRepository<DividendEventJpaEntity, Long> {
    List<DividendEventJpaEntity> findBySymbolOrderByLastDatePriorDesc(String symbol);
    void deleteBySymbol(String symbol);
}
