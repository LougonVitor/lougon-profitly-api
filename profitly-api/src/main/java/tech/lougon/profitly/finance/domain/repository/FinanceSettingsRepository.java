package tech.lougon.profitly.finance.domain.repository;

import tech.lougon.profitly.finance.domain.model.FinanceSettings;

import java.util.Optional;

public interface FinanceSettingsRepository {
    FinanceSettings save(FinanceSettings settings);
    Optional<FinanceSettings> findByUserId(String userId);
}
