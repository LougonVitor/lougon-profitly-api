package tech.lougon.profitly.finance.infrastructure.persistence;

import jakarta.persistence.*;
import tech.lougon.profitly.finance.domain.model.ExpenseType;

import java.math.BigDecimal;

@Entity
@Table(name = "finance_recurring_expenses")
public class RecurringExpenseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String title;

    private BigDecimal estimatedValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseType type;

    public RecurringExpenseJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public BigDecimal getEstimatedValue() { return estimatedValue; }
    public void setEstimatedValue(BigDecimal estimatedValue) { this.estimatedValue = estimatedValue; }
    public ExpenseType getType() { return type; }
    public void setType(ExpenseType type) { this.type = type; }
}
