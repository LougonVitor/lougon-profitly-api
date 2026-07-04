package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * Generic response for the statement endpoints (balance-sheet, income-statement,
 * cash-flow, value-added). Each data row is kept as a generic map so ALL fields are
 * preserved regardless of how many columns brapi adds. Maps (instead of JsonNode)
 * keep this decodable by both Jackson 2 and the Jackson 3 codec used by WebClient.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiStockStatementsResponse(List<Result> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(String symbol, List<Map<String, Object>> data) {}
}
