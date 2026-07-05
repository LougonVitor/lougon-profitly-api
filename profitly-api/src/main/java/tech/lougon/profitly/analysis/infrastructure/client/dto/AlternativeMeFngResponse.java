package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Response of https://api.alternative.me/fng/ — all numeric fields come as strings. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AlternativeMeFngResponse(String name, List<FngEntry> data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FngEntry(
            String value,
            @JsonProperty("value_classification") String valueClassification,
            String timestamp
    ) {}
}
