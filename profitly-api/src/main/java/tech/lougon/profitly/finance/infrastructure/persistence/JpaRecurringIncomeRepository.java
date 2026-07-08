package tech.lougon.profitly.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaRecurringIncomeRepository extends JpaRepository<RecurringIncomeJpaEntity, Long> {
    List<RecurringIncomeJpaEntity> findByUserId(String userId);
    Optional<RecurringIncomeJpaEntity> findByIdAndUserId(Long id, String userId);
}
