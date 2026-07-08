package tech.lougon.profitly.finance.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import tech.lougon.profitly.finance.domain.model.RecurringIncome;
import tech.lougon.profitly.finance.domain.repository.RecurringIncomeRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class RecurringIncomeRepositoryImpl implements RecurringIncomeRepository {

    private final JpaRecurringIncomeRepository jpa;

    public RecurringIncomeRepositoryImpl(JpaRecurringIncomeRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RecurringIncome save(RecurringIncome r) {
        return toDomain(jpa.save(toEntity(r)));
    }

    @Override
    public List<RecurringIncome> findByUserId(String userId) {
        return jpa.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<RecurringIncome> findByIdAndUserId(Long id, String userId) {
        return jpa.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    private RecurringIncomeJpaEntity toEntity(RecurringIncome r) {
        var e = new RecurringIncomeJpaEntity();
        e.setId(r.id());
        e.setUserId(r.userId());
        e.setDescription(r.description());
        e.setAmount(r.amount());
        e.setDueDay(r.dueDay());
        return e;
    }

    private RecurringIncome toDomain(RecurringIncomeJpaEntity e) {
        return new RecurringIncome(e.getId(), e.getUserId(), e.getDescription(), e.getAmount(), e.getDueDay());
    }
}
