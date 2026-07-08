package tech.lougon.profitly.finance.presentation.response;

import tech.lougon.profitly.finance.domain.model.RecurringIncome;

import java.math.BigDecimal;

public record RecurringIncomeResponse(
        Long id,
        String description,
        BigDecimal amount,
        Integer dueDay
) {
    public static RecurringIncomeResponse from(RecurringIncome r) {
        return new RecurringIncomeResponse(r.id(), r.description(), r.amount(), r.dueDay());
    }
}
