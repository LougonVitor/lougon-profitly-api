package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * /api/v2/treasury/indicators — same item shape as /treasury/list, delivered
 * under "results" for up to 20 symbols per call.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiTreasuryIndicatorsResponse(List<BrapiTreasuryListResponse.TreasuryItem> results) {}
