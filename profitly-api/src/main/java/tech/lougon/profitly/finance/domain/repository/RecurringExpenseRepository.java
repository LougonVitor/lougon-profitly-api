package tech.lougon.profitly.finance.domain.repository;

import tech.lougon.profitly.finance.domain.model.RecurringExpense;

import java.util.List;
import java.util.Optional;

public interface RecurringExpenseRepository {
    RecurringExpense save(RecurringExpense recurringExpense);
    List<RecurringExpense> saveAll(List<RecurringExpense> recurringExpenses);
    List<RecurringExpense> findByUserId(String userId);
    Optional<RecurringExpense> findById(Long id);
    void deleteById(Long id);
}
