package tech.lougon.profitly.stock.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiFiiListResponse(
        List<BrapiFii> fiis,
        Pagination pagination
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BrapiFii(
            @JsonProperty("symbol") String symbol,
            @JsonProperty("name")   String name,
            @JsonProperty("price")  BigDecimal price
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pagination(
            @JsonProperty("hasNextPage") boolean hasNextPage
    ) {}
}
