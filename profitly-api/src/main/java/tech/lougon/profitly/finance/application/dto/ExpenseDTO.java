package tech.lougon.profitly.finance.application.dto;

import tech.lougon.profitly.finance.domain.model.Expense;
import tech.lougon.profitly.finance.domain.model.ExpenseStatus;
import tech.lougon.profitly.finance.domain.model.ExpenseType;
import tech.lougon.profitly.finance.domain.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;

public record ExpenseDTO(
        Long id,
        String title,
        String description,
        BigDecimal estimatedValue,
        BigDecimal realValue,
        ExpenseStatus status,
        ExpenseType type,
        PaymentMethod paymentMethod,
        Instant createdAt,
        boolean recurring,
        Long recurringExpenseId
) {
    public static ExpenseDTO from(Expense e) {
        return new ExpenseDTO(e.id(), e.title(), e.description(), e.estimatedValue(),
                e.realValue(), e.status(), e.type(), e.paymentMethod(), e.createdAt(),
                e.recurring(), e.recurringExpenseId());
    }
}
