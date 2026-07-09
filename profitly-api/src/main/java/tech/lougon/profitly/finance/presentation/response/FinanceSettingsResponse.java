package tech.lougon.profitly.finance.presentation.response;

import tech.lougon.profitly.finance.domain.model.FinanceSettings;

import java.math.BigDecimal;

public record FinanceSettingsResponse(
        int resetDay,
        BigDecimal netSalary,
        BigDecimal investmentTarget,
        boolean investmentAuto
) {
    public static FinanceSettingsResponse from(FinanceSettings s) {
        return new FinanceSettingsResponse(s.resetDay(), s.netSalary(), s.investmentTarget(), s.investmentAuto());
    }
}
