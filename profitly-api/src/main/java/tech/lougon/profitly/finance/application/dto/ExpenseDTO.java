package tech.lougon.profitly.finance.application.dto;

import tech.lougon.profitly.finance.domain.model.Expense;
import tech.lougon.profitly.finance.domain.model.ExpenseStatus;
import tech.lougon.profitly.finance.domain.model.ExpenseType;

import java.math.BigDecimal;
import java.time.Instant;

public record ExpenseDTO(
        Long id,
        String title,
        BigDecimal estimatedValue,
        BigDecimal realValue,
        ExpenseStatus status,
        ExpenseType type,
        Instant createdAt,
        boolean recurring
) {
    public static ExpenseDTO from(Expense e) {
        return new ExpenseDTO(e.id(), e.title(), e.estimatedValue(),
                e.realValue(), e.status(), e.type(), e.createdAt(), e.recurring());
    }
}
