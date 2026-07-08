package tech.lougon.profitly.finance.presentation.response;

import tech.lougon.profitly.finance.domain.model.RecurringExpense;

import java.math.BigDecimal;

public record RecurringExpenseResponse(
        Long id,
        String title,
        BigDecimal estimatedValue,
        String type,
        Integer dueDay,
        boolean variable
) {
    public static RecurringExpenseResponse from(RecurringExpense r) {
        return new RecurringExpenseResponse(r.id(), r.title(), r.estimatedValue(), r.type().name(),
                r.dueDay(), r.variable());
    }
}
