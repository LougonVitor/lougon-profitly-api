package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface JpaFundNavHistoryRepository extends JpaRepository<FundNavHistoryJpaEntity, Long> {
    List<FundNavHistoryJpaEntity> findBySymbolOrderByReferenceDateAsc(String symbol);
    List<FundNavHistoryJpaEntity> findTop2BySymbolOrderByReferenceDateDesc(String symbol);

    @Query("SELECT COUNT(h) FROM FundNavHistoryJpaEntity h WHERE h.symbol = :symbol")
    long countBySymbol(@Param("symbol") String symbol);
}
