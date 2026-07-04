package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Response of /api/v2/crypto/available — plain list of coin symbols. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiCryptoAvailableResponse(List<String> coins) {}
