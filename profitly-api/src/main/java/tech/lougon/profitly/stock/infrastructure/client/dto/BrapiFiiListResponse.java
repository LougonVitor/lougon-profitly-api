package tech.lougon.profitly.stock.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiFiiListResponse(
        List<BrapiFii> fiis,
        Pagination pagination
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BrapiFii(String symbol) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pagination(boolean hasNextPage) {}
}
