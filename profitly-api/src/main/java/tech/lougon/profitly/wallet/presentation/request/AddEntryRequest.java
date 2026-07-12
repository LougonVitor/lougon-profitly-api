package tech.lougon.profitly.wallet.presentation.request;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AddEntryRequest(
        LocalDate date,
        BigDecimal quantity,
        BigDecimal paidPrice,
        String type
) {}
