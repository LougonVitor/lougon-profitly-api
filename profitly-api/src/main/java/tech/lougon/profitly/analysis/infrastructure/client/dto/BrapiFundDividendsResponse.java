package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiFundDividendsResponse(List<FundDividend> dividends) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FundDividend(
            String symbol,
            String approvedOn,
            String label,
            String lastDatePrior,
            String paymentDate,
            Double rate,
            String relatedTo,
            String remarks
    ) {}
}
