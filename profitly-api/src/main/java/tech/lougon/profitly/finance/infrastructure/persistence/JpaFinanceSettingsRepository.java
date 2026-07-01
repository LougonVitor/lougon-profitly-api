package tech.lougon.profitly.finance.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaFinanceSettingsRepository extends JpaRepository<FinanceSettingsJpaEntity, String> {}
