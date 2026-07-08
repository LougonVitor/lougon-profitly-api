package tech.lougon.profitly.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaAdditionalIncomeRepository extends JpaRepository<AdditionalIncomeJpaEntity, Long> {
    List<AdditionalIncomeJpaEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<AdditionalIncomeJpaEntity> findByIdAndUserId(Long id, String userId);
}
