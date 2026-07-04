package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiDividendsResponse(
        @JsonProperty("results") List<Result> results
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(
            @JsonProperty("symbol") String symbol,
            @JsonProperty("data") Data data
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            @JsonProperty("cashDividends") List<CashDividend> cashDividends,
            @JsonProperty("stockDividends") List<StockDividend> stockDividends
    ) {}

    /** Splits, reverse splits and bonus issues — needed to adjust dividend rates for DY. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StockDividend(
            @JsonProperty("assetIssued")    String assetIssued,
            @JsonProperty("factor")         Double factor,
            @JsonProperty("completeFactor") String completeFactor,
            @JsonProperty("approvedOn")     String approvedOn,
            @JsonProperty("isinCode")       String isinCode,
            @JsonProperty("label")          String label,
            @JsonProperty("lastDatePrior")  String lastDatePrior,
            @JsonProperty("remarks")        String remarks
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CashDividend(
            @JsonProperty("assetIssued")  String assetIssued,
            @JsonProperty("paymentDate")  String paymentDate,
            @JsonProperty("rate")         Double rate,
            @JsonProperty("relatedTo")    String relatedTo,
            @JsonProperty("approvedOn")   String approvedOn,
            @JsonProperty("isinCode")     String isinCode,
            @JsonProperty("label")        String label,
            @JsonProperty("lastDatePrior") String lastDatePrior,
            @JsonProperty("remarks")      String remarks
    ) {}
}
