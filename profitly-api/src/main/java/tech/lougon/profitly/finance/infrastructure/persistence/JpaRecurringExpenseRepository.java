package tech.lougon.profitly.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaRecurringExpenseRepository extends JpaRepository<RecurringExpenseJpaEntity, Long> {
    List<RecurringExpenseJpaEntity> findByUserId(String userId);
}
