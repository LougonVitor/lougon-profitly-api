package tech.lougon.profitly.finance.presentation.request;

import tech.lougon.profitly.finance.domain.model.ExpenseStatus;
import tech.lougon.profitly.finance.domain.model.ExpenseType;

import java.math.BigDecimal;

public record UpdateExpenseRequest(
        String title,
        BigDecimal estimatedValue,
        BigDecimal realValue,
        ExpenseStatus status,
        ExpenseType type
) {}
