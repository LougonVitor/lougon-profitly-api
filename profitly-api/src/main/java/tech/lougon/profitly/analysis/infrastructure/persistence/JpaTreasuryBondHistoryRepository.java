package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface JpaTreasuryBondHistoryRepository extends JpaRepository<TreasuryBondHistoryJpaEntity, Long> {
    List<TreasuryBondHistoryJpaEntity> findBySymbolOrderByReferenceDateAsc(String symbol);
    Optional<TreasuryBondHistoryJpaEntity> findTopBySymbolOrderByReferenceDateDesc(String symbol);

    @Query("SELECT COUNT(h) FROM TreasuryBondHistoryJpaEntity h WHERE h.symbol = :symbol")
    long countBySymbol(@Param("symbol") String symbol);
}
