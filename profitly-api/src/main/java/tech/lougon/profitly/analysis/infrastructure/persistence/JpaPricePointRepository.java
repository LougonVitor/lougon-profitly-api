package tech.lougon.profitly.analysis.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaPricePointRepository extends JpaRepository<PricePointJpaEntity, Long> {

    List<PricePointJpaEntity> findBySymbolAndDateBetweenOrderByDateAsc(String symbol, LocalDate from, LocalDate to);

    @Query("SELECT MAX(p.date) FROM PricePointJpaEntity p WHERE p.symbol = :symbol")
    Optional<LocalDate> findLatestDateBySymbol(String symbol);

    // Returns [year (int), avgClose] grouped by calendar year for a given symbol
    @Query(value = """
            SELECT EXTRACT(YEAR FROM p.date) AS yr, AVG(p.close) AS avg_close
            FROM price_points p
            WHERE p.symbol = :symbol
            GROUP BY EXTRACT(YEAR FROM p.date)
            ORDER BY yr
            """, nativeQuery = true)
    List<Object[]> avgAnnualCloseBySymbol(@Param("symbol") String symbol);

    // Symbols that have dividend_events but no price_points at all
    @Query(value = """
            SELECT DISTINCT d.symbol
            FROM dividend_events d
            WHERE d.symbol NOT IN (SELECT DISTINCT p.symbol FROM price_points p)
            """, nativeQuery = true)
    List<String> findSymbolsWithDividendsButNoPriceHistory();
}
