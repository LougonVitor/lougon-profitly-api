package tech.lougon.profitly.finance.domain.repository;

import tech.lougon.profitly.finance.domain.model.BudgetLimit;
import tech.lougon.profitly.finance.domain.model.ExpenseType;

import java.util.List;
import java.util.Optional;

public interface BudgetLimitRepository {
    BudgetLimit save(BudgetLimit limit);
    List<BudgetLimit> findByUserId(String userId);
    Optional<BudgetLimit> findByUserIdAndType(String userId, ExpenseType type);
    void deleteByUserIdAndType(String userId, ExpenseType type);
}
