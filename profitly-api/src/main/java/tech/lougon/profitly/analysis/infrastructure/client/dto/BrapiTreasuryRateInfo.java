package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Explains what buyRate/sellRate mean for a bond. For Tesouro Selic the rates
 * are a spread over the Selic rate (rateType "spreadOverSelic"), not the full yield.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiTreasuryRateInfo(
        String rateType,
        String rateUnit,
        String description
) {}
