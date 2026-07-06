package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface JpaFiiDividendEventRepository extends JpaRepository<FiiDividendEventJpaEntity, Long> {
    List<FiiDividendEventJpaEntity> findBySymbolOrderByPaymentDateDesc(String symbol);

    @Query("SELECT COUNT(d) FROM FiiDividendEventJpaEntity d WHERE d.symbol = :symbol")
    long countBySymbol(@Param("symbol") String symbol);
}
