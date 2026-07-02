package tech.lougon.profitly.wallet.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddDividendRequest(
        @NotBlank String ticker,
        @NotNull BigDecimal totalAmount,
        @NotNull LocalDate paymentDate,
        @NotBlank String type,
        boolean received
) {}
