package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaPricePointRepository extends JpaRepository<PricePointJpaEntity, Long> {

    List<PricePointJpaEntity> findBySymbolAndDateBetweenOrderByDateAsc(String symbol, LocalDate from, LocalDate to);

    @Query("SELECT MAX(p.date) FROM PricePointJpaEntity p WHERE p.symbol = :symbol")
    Optional<LocalDate> findLatestDateBySymbol(String symbol);
}
