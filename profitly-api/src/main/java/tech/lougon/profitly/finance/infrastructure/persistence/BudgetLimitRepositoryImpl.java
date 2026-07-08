package tech.lougon.profitly.finance.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import tech.lougon.profitly.finance.domain.model.BudgetLimit;
import tech.lougon.profitly.finance.domain.model.ExpenseType;
import tech.lougon.profitly.finance.domain.repository.BudgetLimitRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class BudgetLimitRepositoryImpl implements BudgetLimitRepository {

    private final JpaBudgetLimitRepository jpa;

    public BudgetLimitRepositoryImpl(JpaBudgetLimitRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public BudgetLimit save(BudgetLimit limit) {
        return toDomain(jpa.save(toEntity(limit)));
    }

    @Override
    public List<BudgetLimit> findByUserId(String userId) {
        return jpa.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<BudgetLimit> findByUserIdAndType(String userId, ExpenseType type) {
        return jpa.findByUserIdAndType(userId, type).map(this::toDomain);
    }

    @Override
    public void deleteByUserIdAndType(String userId, ExpenseType type) {
        jpa.deleteByUserIdAndType(userId, type);
    }

    private BudgetLimitJpaEntity toEntity(BudgetLimit b) {
        var e = new BudgetLimitJpaEntity();
        e.setId(b.id());
        e.setUserId(b.userId());
        e.setType(b.type());
        e.setMonthlyLimit(b.monthlyLimit());
        return e;
    }

    private BudgetLimit toDomain(BudgetLimitJpaEntity e) {
        return new BudgetLimit(e.getId(), e.getUserId(), e.getType(), e.getMonthlyLimit());
    }
}
