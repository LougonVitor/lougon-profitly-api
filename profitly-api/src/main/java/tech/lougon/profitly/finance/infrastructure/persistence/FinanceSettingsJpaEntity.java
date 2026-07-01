package tech.lougon.profitly.finance.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "finance_settings")
public class FinanceSettingsJpaEntity {

    @Id
    private String userId;

    @Column(nullable = false)
    private int resetDay = 10;

    private BigDecimal netSalary;
    private BigDecimal investmentTarget;

    public FinanceSettingsJpaEntity() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getResetDay() { return resetDay; }
    public void setResetDay(int resetDay) { this.resetDay = resetDay; }
    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }
    public BigDecimal getInvestmentTarget() { return investmentTarget; }
    public void setInvestmentTarget(BigDecimal investmentTarget) { this.investmentTarget = investmentTarget; }
}
