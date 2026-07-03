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

    // Returns [symbol, year (string), sumRate] grouped by symbol + year extracted from paymentDate
    @Query(value = """
            SELECT e.symbol,
                   SUBSTRING(e.payment_date, 1, 4) AS yr,
                   SUM(e.rate)                      AS total
            FROM dividend_events e
            WHERE e.rate > 0
              AND e.payment_date IS NOT NULL
              AND LENGTH(e.payment_date) >= 4
            GROUP BY e.symbol, SUBSTRING(e.payment_date, 1, 4)
            """, nativeQuery = true)
    List<Object[]> sumBySymbolAndYear();
}
