package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/**
 * Generic response for the FII document endpoints whose row shape is rich and
 * varies by segment: /fii/properties, /fii/properties/history, /fii/portfolio,
 * /fii/portfolio/history. Rows are kept as generic maps — the raw JSON pattern —
 * so nothing is lost when brapi adds fields. Maps (instead of JsonNode) stay
 * decodable by the Jackson 3 codec used by WebClient.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiFiiRawListResponse(
        @JsonAlias({"fiis", "history"}) List<Map<String, Object>> items
) {}
