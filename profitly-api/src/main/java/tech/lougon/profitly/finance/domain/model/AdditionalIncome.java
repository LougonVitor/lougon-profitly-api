package tech.lougon.profitly.finance.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record AdditionalIncome(Long id, String userId, String description, BigDecimal amount, Instant createdAt) {}
