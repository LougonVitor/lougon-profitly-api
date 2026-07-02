package tech.lougon.profitly.finance.domain.repository;

import tech.lougon.profitly.finance.domain.model.ExpenseHistorySummary;

import java.util.List;

public interface ExpenseHistoryRepository {
    void saveAll(List<ExpenseHistorySummary> summaries);
    List<ExpenseHistorySummary> findByUserIdAndYearMonthBetween(String userId, String from, String to);
    List<String> findDistinctYearMonthsByUserId(String userId);
    boolean existsByUserIdAndYearMonth(String userId, String yearMonth);
    void deleteByUserIdAndYearMonth(String userId, String yearMonth);
    void deleteOlderThan(String userId, String cutoff);
}
