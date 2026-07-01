package tech.lougon.profitly.finance.application.dto;

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
        int resetDay
) {
    public static CurrentPeriodDTO from(List<Expense> expenses, FinanceSettings settings) {
        BigDecimal totalReal = expenses.stream()
                .map(Expense::realValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEstimated = expenses.stream()
                .map(e -> e.estimatedValue() != null ? e.estimatedValue() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal salary = settings.netSalary() != null ? settings.netSalary() : BigDecimal.ZERO;
        BigDecimal investment = settings.investmentTarget() != null ? settings.investmentTarget() : BigDecimal.ZERO;
        BigDecimal balance = salary.subtract(investment).subtract(totalReal);

        List<ExpenseDTO> dtos = expenses.stream().map(ExpenseDTO::from).toList();
        return new CurrentPeriodDTO(dtos, settings.netSalary(), settings.investmentTarget(),
                totalReal, totalEstimated, balance, settings.resetDay());
    }
}
