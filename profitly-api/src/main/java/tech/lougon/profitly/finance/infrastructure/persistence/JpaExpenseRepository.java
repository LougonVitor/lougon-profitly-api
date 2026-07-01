package tech.lougon.profitly.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.lougon.profitly.finance.domain.model.ExpenseType;

import java.util.List;
import java.util.Optional;

public interface JpaExpenseRepository extends JpaRepository<ExpenseJpaEntity, Long> {
    List<ExpenseJpaEntity> findByUserIdOrderByCreatedAtAsc(String userId);
    Optional<ExpenseJpaEntity> findByUserIdAndTitleIgnoreCase(String userId, String title);
    List<ExpenseJpaEntity> findByUserIdAndTypeOrderByCreatedAtAsc(String userId, ExpenseType type);
}
