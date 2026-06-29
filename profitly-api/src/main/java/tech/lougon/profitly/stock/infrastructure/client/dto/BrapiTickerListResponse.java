package tech.lougon.profitly.stock.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiTickerListResponse(
        List<BrapiTicker> stocks
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BrapiTicker(
            String stock,
            String name,
            String type
    ) {}
}
