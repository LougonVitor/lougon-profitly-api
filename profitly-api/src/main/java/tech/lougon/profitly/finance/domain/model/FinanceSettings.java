package tech.lougon.profitly.finance.domain.model;

import java.math.BigDecimal;

public record FinanceSettings(
        String userId,
        int resetDay,
        BigDecimal netSalary,
        BigDecimal investmentTarget,
        boolean investmentAuto,
        BigDecimal savingsTarget
) {}
