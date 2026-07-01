package tech.lougon.profitly.finance.domain.repository;

import tech.lougon.profitly.finance.domain.model.Expense;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository {
    Expense save(Expense expense);
    Optional<Expense> findById(Long id);
    List<Expense> findByUserId(String userId);
    Optional<Expense> findByUserIdAndTitle(String userId, String title);
    void deleteById(Long id);
}
