package tech.lougon.profitly.finance.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record QuickLaunchRequest(@NotBlank String title, @NotNull BigDecimal value) {}
