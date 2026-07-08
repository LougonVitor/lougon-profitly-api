package tech.lougon.profitly.finance.application.dto;

import tech.lougon.profitly.finance.domain.model.BudgetLimit;
import tech.lougon.profitly.finance.domain.model.ExpenseType;

import java.math.BigDecimal;

public record BudgetLimitDTO(ExpenseType type, BigDecimal monthlyLimit) {
    public static BudgetLimitDTO from(BudgetLimit b) {
        return new BudgetLimitDTO(b.type(), b.monthlyLimit());
    }
}
