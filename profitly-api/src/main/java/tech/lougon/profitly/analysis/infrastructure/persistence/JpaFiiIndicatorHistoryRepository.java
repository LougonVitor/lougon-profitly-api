package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaFiiIndicatorHistoryRepository extends JpaRepository<FiiIndicatorHistoryJpaEntity, Long> {

    List<FiiIndicatorHistoryJpaEntity> findBySymbolOrderByReferenceDateAsc(String symbol);

    Optional<FiiIndicatorHistoryJpaEntity> findTopBySymbolOrderByReferenceDateDesc(String symbol);

    @Query("SELECT COUNT(h) FROM FiiIndicatorHistoryJpaEntity h WHERE h.symbol = :symbol")
    long countBySymbol(@Param("symbol") String symbol);
}
