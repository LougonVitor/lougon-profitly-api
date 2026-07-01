package tech.lougon.profitly.finance.presentation.request;

import java.math.BigDecimal;

public record FinanceSettingsRequest(Integer resetDay, BigDecimal netSalary, BigDecimal investmentTarget) {}
