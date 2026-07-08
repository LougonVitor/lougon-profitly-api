package tech.lougon.profitly.finance.domain.repository;

import tech.lougon.profitly.finance.domain.model.RecurringIncome;

import java.util.List;
import java.util.Optional;

public interface RecurringIncomeRepository {
    RecurringIncome save(RecurringIncome recurringIncome);
    List<RecurringIncome> findByUserId(String userId);
    Optional<RecurringIncome> findByIdAndUserId(Long id, String userId);
    void deleteById(Long id);
}
