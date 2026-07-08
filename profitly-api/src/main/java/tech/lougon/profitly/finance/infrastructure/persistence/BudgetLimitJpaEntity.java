package tech.lougon.profitly.finance.infrastructure.persistence;

import jakarta.persistence.*;
import tech.lougon.profitly.finance.domain.model.ExpenseType;

import java.math.BigDecimal;

@Entity
@Table(name = "finance_budget_limits",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "type"}))
public class BudgetLimitJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseType type;

    @Column(nullable = false)
    private BigDecimal monthlyLimit;

    public BudgetLimitJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public ExpenseType getType() { return type; }
    public void setType(ExpenseType type) { this.type = type; }
    public BigDecimal getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(BigDecimal monthlyLimit) { this.monthlyLimit = monthlyLimit; }
}
