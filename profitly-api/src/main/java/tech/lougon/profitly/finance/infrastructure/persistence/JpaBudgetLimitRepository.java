package tech.lougon.profitly.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.lougon.profitly.finance.domain.model.ExpenseType;

import java.util.List;
import java.util.Optional;

public interface JpaBudgetLimitRepository extends JpaRepository<BudgetLimitJpaEntity, Long> {
    List<BudgetLimitJpaEntity> findByUserId(String userId);
    Optional<BudgetLimitJpaEntity> findByUserIdAndType(String userId, ExpenseType type);
    void deleteByUserIdAndType(String userId, ExpenseType type);
}
