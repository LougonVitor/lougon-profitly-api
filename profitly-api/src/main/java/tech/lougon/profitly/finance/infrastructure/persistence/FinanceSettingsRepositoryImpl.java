package tech.lougon.profitly.finance.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import tech.lougon.profitly.finance.domain.model.FinanceSettings;
import tech.lougon.profitly.finance.domain.repository.FinanceSettingsRepository;

import java.util.Optional;

@Repository
public class FinanceSettingsRepositoryImpl implements FinanceSettingsRepository {

    private final JpaFinanceSettingsRepository jpa;

    public FinanceSettingsRepositoryImpl(JpaFinanceSettingsRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public FinanceSettings save(FinanceSettings s) {
        return toDomain(jpa.save(toEntity(s)));
    }

    @Override
    public Optional<FinanceSettings> findByUserId(String userId) {
        return jpa.findById(userId).map(this::toDomain);
    }

    private FinanceSettingsJpaEntity toEntity(FinanceSettings s) {
        var e = new FinanceSettingsJpaEntity();
        e.setUserId(s.userId());
        e.setResetDay(s.resetDay());
        e.setNetSalary(s.netSalary());
        e.setInvestmentTarget(s.investmentTarget());
        return e;
    }

    private FinanceSettings toDomain(FinanceSettingsJpaEntity e) {
        return new FinanceSettings(e.getUserId(), e.getResetDay(), e.getNetSalary(), e.getInvestmentTarget());
    }
}
