package tech.lougon.profitly.finance.presentation.request;

import jakarta.validation.constraints.Size;
import tech.lougon.profitly.finance.domain.model.ExpenseStatus;
import tech.lougon.profitly.finance.domain.model.ExpenseType;
import tech.lougon.profitly.finance.domain.model.PaymentMethod;

import java.math.BigDecimal;

public record UpdateExpenseRequest(
        String title,
        @Size(max = 500) String description,
        BigDecimal estimatedValue,
        BigDecimal realValue,
        ExpenseStatus status,
        ExpenseType type,
        PaymentMethod paymentMethod
) {}
