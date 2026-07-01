package tech.lougon.profitly.finance.infrastructure.persistence;

import jakarta.persistence.*;
import tech.lougon.profitly.finance.domain.model.ExpenseType;

import java.math.BigDecimal;

@Entity
@Table(name = "finance_expense_history",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "yearMonth", "type"}))
public class ExpenseHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String yearMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseType type;

    @Column(nullable = false)
    private BigDecimal totalReal;

    private BigDecimal totalEstimated;

    public ExpenseHistoryJpaEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }
    public ExpenseType getType() { return type; }
    public void setType(ExpenseType type) { this.type = type; }
    public BigDecimal getTotalReal() { return totalReal; }
    public void setTotalReal(BigDecimal totalReal) { this.totalReal = totalReal; }
    public BigDecimal getTotalEstimated() { return totalEstimated; }
    public void setTotalEstimated(BigDecimal totalEstimated) { this.totalEstimated = totalEstimated; }
}
