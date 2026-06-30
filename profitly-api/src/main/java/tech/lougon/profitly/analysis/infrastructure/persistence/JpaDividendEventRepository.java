package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaDividendEventRepository extends JpaRepository<DividendEventJpaEntity, Long> {
    List<DividendEventJpaEntity> findBySymbolOrderByLastDatePriorDesc(String symbol);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM DividendEventJpaEntity e WHERE e.symbol = :symbol")
    void deleteBySymbol(@Param("symbol") String symbol);
}
