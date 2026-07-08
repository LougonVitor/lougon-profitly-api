package tech.lougon.profitly.finance.domain.model;

import java.math.BigDecimal;

/** A template income (salary top-up, allowance, etc.) injected into each period. */
public record RecurringIncome(
        Long id,
        String userId,
        String description,
        BigDecimal amount,
        Integer dueDay
) {}
