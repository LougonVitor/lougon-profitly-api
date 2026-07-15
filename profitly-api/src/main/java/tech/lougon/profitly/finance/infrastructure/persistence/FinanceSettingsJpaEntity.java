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

    // true = "Investimento" real vem das compras da carteira; false = valor manual editável
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean investmentAuto = true;

    // Quanto o usuário pretende guardar por mês. Null = sem meta definida.
    private BigDecimal savingsTarget;

    public FinanceSettingsJpaEntity() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getResetDay() { return resetDay; }
    public void setResetDay(int resetDay) { this.resetDay = resetDay; }
    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }
    public BigDecimal getInvestmentTarget() { return investmentTarget; }
    public void setInvestmentTarget(BigDecimal investmentTarget) { this.investmentTarget = investmentTarget; }
    public boolean isInvestmentAuto() { return investmentAuto; }
    public void setInvestmentAuto(boolean investmentAuto) { this.investmentAuto = investmentAuto; }
    public BigDecimal getSavingsTarget() { return savingsTarget; }
    public void setSavingsTarget(BigDecimal savingsTarget) { this.savingsTarget = savingsTarget; }
}
