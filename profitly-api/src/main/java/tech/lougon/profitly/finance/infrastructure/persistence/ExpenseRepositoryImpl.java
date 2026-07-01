package tech.lougon.profitly.finance.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import tech.lougon.profitly.finance.domain.model.Expense;
import tech.lougon.profitly.finance.domain.repository.ExpenseRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class ExpenseRepositoryImpl implements ExpenseRepository {

    private final JpaExpenseRepository jpa;

    public ExpenseRepositoryImpl(JpaExpenseRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Expense save(Expense expense) {
        return toDomain(jpa.save(toEntity(expense)));
    }

    @Override
    public Optional<Expense> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public List<Expense> findByUserId(String userId) {
        return jpa.findByUserIdOrderByCreatedAtAsc(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Expense> findByUserIdAndTitle(String userId, String title) {
        return jpa.findByUserIdAndTitleIgnoreCase(userId, title).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    private ExpenseJpaEntity toEntity(Expense e) {
        var entity = new ExpenseJpaEntity();
        entity.setId(e.id());
        entity.setUserId(e.userId());
        entity.setTitle(e.title());
        entity.setEstimatedValue(e.estimatedValue());
        entity.setRealValue(e.realValue());
        entity.setStatus(e.status());
        entity.setType(e.type());
        entity.setCreatedAt(e.createdAt());
        return entity;
    }

    private Expense toDomain(ExpenseJpaEntity e) {
        return new Expense(e.getId(), e.getUserId(), e.getTitle(),
                e.getEstimatedValue(), e.getRealValue(), e.getStatus(), e.getType(), e.getCreatedAt());
    }
}
