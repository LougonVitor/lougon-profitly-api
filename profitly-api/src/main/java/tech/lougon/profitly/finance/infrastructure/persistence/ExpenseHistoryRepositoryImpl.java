package tech.lougon.profitly.finance.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import tech.lougon.profitly.finance.domain.model.ExpenseHistorySummary;
import tech.lougon.profitly.finance.domain.repository.ExpenseHistoryRepository;

import java.util.List;

@Repository
public class ExpenseHistoryRepositoryImpl implements ExpenseHistoryRepository {

    private final JpaExpenseHistoryRepository jpa;

    public ExpenseHistoryRepositoryImpl(JpaExpenseHistoryRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void saveAll(List<ExpenseHistorySummary> summaries) {
        jpa.saveAll(summaries.stream().map(this::toEntity).toList());
    }

    @Override
    public List<ExpenseHistorySummary> findByUserIdAndYearMonthBetween(String userId, String from, String to) {
        return jpa.findByUserIdAndYearMonthBetweenOrderByYearMonthAsc(userId, from, to)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<String> findDistinctYearMonthsByUserId(String userId) {
        return jpa.findDistinctYearMonthsByUserId(userId);
    }

    @Override
    public boolean existsByUserIdAndYearMonth(String userId, String yearMonth) {
        return jpa.existsByUserIdAndYearMonth(userId, yearMonth);
    }

    @Override
    public void deleteByUserIdAndYearMonth(String userId, String yearMonth) {
        jpa.deleteByUserIdAndYearMonth(userId, yearMonth);
    }

    @Override
    public void deleteOlderThan(String userId, String cutoff) {
        jpa.deleteByUserIdAndYearMonthLessThan(userId, cutoff);
    }

    private ExpenseHistoryJpaEntity toEntity(ExpenseHistorySummary s) {
        var e = new ExpenseHistoryJpaEntity();
        e.setId(s.id());
        e.setUserId(s.userId());
        e.setYearMonth(s.yearMonth());
        e.setType(s.type());
        e.setTotalReal(s.totalReal());
        e.setTotalEstimated(s.totalEstimated());
        return e;
    }

    private ExpenseHistorySummary toDomain(ExpenseHistoryJpaEntity e) {
        return new ExpenseHistorySummary(e.getId(), e.getUserId(), e.getYearMonth(),
                e.getType(), e.getTotalReal(), e.getTotalEstimated());
    }
}
