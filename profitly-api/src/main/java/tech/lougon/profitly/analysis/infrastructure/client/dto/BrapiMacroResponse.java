package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiMacroResponse(List<SeriesResult> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeriesResult(SeriesMeta series, List<Observation> observations) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeriesMeta(String slug, String name, String unit, String frequency, String category) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Observation(String date, Double value) {}
}
