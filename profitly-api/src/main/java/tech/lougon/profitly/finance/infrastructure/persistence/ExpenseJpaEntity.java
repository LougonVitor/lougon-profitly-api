package tech.lougon.profitly.finance.infrastructure.persistence;

import jakarta.persistence.*;
import tech.lougon.profitly.finance.domain.model.ExpenseStatus;
import tech.lougon.profitly.finance.domain.model.ExpenseType;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "finance_expenses")
public class ExpenseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String title;

    private BigDecimal estimatedValue;

    @Column(nullable = false)
    private BigDecimal realValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseType type;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean recurring;

    // Links an auto-populated expense back to the recurring template that spawned it.
    // Null for one-off (avulso) expenses and the special investment row.
    private Long recurringExpenseId;

    public ExpenseJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public BigDecimal getEstimatedValue() { return estimatedValue; }
    public void setEstimatedValue(BigDecimal estimatedValue) { this.estimatedValue = estimatedValue; }
    public BigDecimal getRealValue() { return realValue; }
    public void setRealValue(BigDecimal realValue) { this.realValue = realValue; }
    public ExpenseStatus getStatus() { return status; }
    public void setStatus(ExpenseStatus status) { this.status = status; }
    public ExpenseType getType() { return type; }
    public void setType(ExpenseType type) { this.type = type; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }
    public Long getRecurringExpenseId() { return recurringExpenseId; }
    public void setRecurringExpenseId(Long recurringExpenseId) { this.recurringExpenseId = recurringExpenseId; }
}
