package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/**
 * Generic response for the fund document endpoints whose row shape varies per fund
 * type: /funds/profile ("profiles"), /funds/portfolio and /funds/{fiagro,fidc}/portfolio
 * ("funds"), /funds/{fiagro,fidc,fip}/reports ("reports"). Rows are kept as generic
 * maps — the raw JSON pattern — so nothing is lost when brapi adds fields. Maps
 * (instead of JsonNode) stay decodable by the Jackson 3 codec used by WebClient.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiFundRawListResponse(
        @JsonAlias({"funds", "profiles", "reports"}) List<Map<String, Object>> items,
        Pagination pagination
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pagination(
            Integer page,
            Integer totalItems,
            Integer totalPages,
            Boolean hasNextPage
    ) {}
}
