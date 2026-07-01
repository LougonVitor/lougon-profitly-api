package tech.lougon.profitly.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaAdditionalIncomeRepository extends JpaRepository<AdditionalIncomeJpaEntity, Long> {
    List<AdditionalIncomeJpaEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}
