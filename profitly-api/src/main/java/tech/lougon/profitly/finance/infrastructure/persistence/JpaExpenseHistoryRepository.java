package tech.lougon.profitly.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JpaExpenseHistoryRepository extends JpaRepository<ExpenseHistoryJpaEntity, Long> {
    List<ExpenseHistoryJpaEntity> findByUserIdAndYearMonthBetweenOrderByYearMonthAsc(String userId, String from, String to);

    @Query("SELECT DISTINCT e.yearMonth FROM ExpenseHistoryJpaEntity e WHERE e.userId = :userId ORDER BY e.yearMonth ASC")
    List<String> findDistinctYearMonthsByUserId(String userId);

    boolean existsByUserIdAndYearMonth(String userId, String yearMonth);
    void deleteByUserId(String userId);
    void deleteByUserIdAndYearMonth(String userId, String yearMonth);
    void deleteByUserIdAndYearMonthLessThan(String userId, String cutoff);
}
