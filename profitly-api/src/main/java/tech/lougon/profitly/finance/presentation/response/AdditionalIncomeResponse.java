package tech.lougon.profitly.finance.presentation.response;

import tech.lougon.profitly.finance.domain.model.AdditionalIncome;

import java.math.BigDecimal;
import java.time.Instant;

public record AdditionalIncomeResponse(
        Long id,
        String description,
        BigDecimal amount,
        Instant createdAt
) {
    public static AdditionalIncomeResponse from(AdditionalIncome a) {
        return new AdditionalIncomeResponse(a.id(), a.description(), a.amount(), a.createdAt());
    }
}
