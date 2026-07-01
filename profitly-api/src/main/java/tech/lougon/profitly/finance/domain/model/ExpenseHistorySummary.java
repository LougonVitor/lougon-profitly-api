package tech.lougon.profitly.finance.domain.model;

import java.math.BigDecimal;

public record ExpenseHistorySummary(
        Long id,
        String userId,
        String yearMonth,
        ExpenseType type,
        BigDecimal totalReal,
        BigDecimal totalEstimated
) {}
