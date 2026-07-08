package tech.lougon.profitly.finance.application.dto;

import tech.lougon.profitly.finance.domain.model.AdditionalIncome;
import tech.lougon.profitly.finance.domain.model.BudgetLimit;
import tech.lougon.profitly.finance.domain.model.Expense;
import tech.lougon.profitly.finance.domain.model.FinanceSettings;

import java.math.BigDecimal;
import java.util.List;

public record CurrentPeriodDTO(
        List<ExpenseDTO> expenses,
        BigDecimal netSalary,
        BigDecimal investmentTarget,
        BigDecimal totalReal,
        BigDecimal totalEstimated,
        BigDecimal balance,
        int resetDay,
        List<AdditionalIncome> additionalIncomes,
        BigDecimal totalIncome,
        List<BudgetLimitDTO> budgetLimits,
        BigDecimal investedThisMonth
) {
    public static CurrentPeriodDTO from(List<Expense> expenses, FinanceSettings settings,
                                        List<AdditionalIncome> additionalIncomes,
                                        List<BudgetLimit> budgetLimits,
                                        BigDecimal investedThisMonth) {
        BigDecimal totalReal = expenses.stream()
                .map(Expense::realValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEstimated = expenses.stream()
                .map(e -> e.estimatedValue() != null ? e.estimatedValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal salary = settings.netSalary() != null ? settings.netSalary() : BigDecimal.ZERO;
        BigDecimal additionalTotal = additionalIncomes.stream()
                .map(AdditionalIncome::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIncome = salary.add(additionalTotal);
        // Investment is already an expense row in totalReal — do not subtract investmentTarget separately
        BigDecimal balance = totalIncome.subtract(totalReal);

        List<ExpenseDTO> dtos = expenses.stream().map(ExpenseDTO::from).toList();
        List<BudgetLimitDTO> limitDtos = budgetLimits.stream().map(BudgetLimitDTO::from).toList();
        return new CurrentPeriodDTO(dtos, settings.netSalary(), settings.investmentTarget(),
                totalReal, totalEstimated, balance, settings.resetDay(), additionalIncomes, totalIncome,
                limitDtos, investedThisMonth);
    }
}
