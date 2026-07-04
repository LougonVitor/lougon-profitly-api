package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Generic response for the statement endpoints (balance-sheet, income-statement,
 * cash-flow, value-added). Each data row is kept as raw JSON so ALL fields are
 * preserved in the database regardless of how many columns brapi adds.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiStockStatementsResponse(List<Result> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(String symbol, List<JsonNode> data) {}
}
