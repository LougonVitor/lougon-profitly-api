package tech.lougon.profitly.finance.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import tech.lougon.profitly.finance.domain.model.RecurringExpense;
import tech.lougon.profitly.finance.domain.repository.RecurringExpenseRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class RecurringExpenseRepositoryImpl implements RecurringExpenseRepository {

    private final JpaRecurringExpenseRepository jpa;

    public RecurringExpenseRepositoryImpl(JpaRecurringExpenseRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public RecurringExpense save(RecurringExpense recurringExpense) {
        return toDomain(jpa.save(toEntity(recurringExpense)));
    }

    @Override
    public List<RecurringExpense> saveAll(List<RecurringExpense> recurringExpenses) {
        return jpa.saveAll(recurringExpenses.stream().map(this::toEntity).toList())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<RecurringExpense> findByUserId(String userId) {
        return jpa.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<RecurringExpense> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    private RecurringExpenseJpaEntity toEntity(RecurringExpense r) {
        var e = new RecurringExpenseJpaEntity();
        e.setId(r.id());
        e.setUserId(r.userId());
        e.setTitle(r.title());
        e.setEstimatedValue(r.estimatedValue());
        e.setType(r.type());
        return e;
    }

    private RecurringExpense toDomain(RecurringExpenseJpaEntity e) {
        return new RecurringExpense(e.getId(), e.getUserId(), e.getTitle(), e.getEstimatedValue(), e.getType());
    }
}
