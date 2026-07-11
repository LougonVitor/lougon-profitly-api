package tech.lougon.profitly.wallet.presentation.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateEntryRequest(
        LocalDate date,
        Integer quantity,
        BigDecimal paidPrice,
        String type
) {}
