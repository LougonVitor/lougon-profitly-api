package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Response of /api/v2/funds/dividends — root key is "dividends" (flat list). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiFundDividendsResponse(List<FundDividend> dividends, Pagination pagination) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FundDividend(
            String symbol,
            String cnpj,
            String assetType,
            String declaredDate,
            String approvedOn,
            String label,
            String lastDatePrior,
            String paymentDate,
            Double rate,
            String relatedTo,
            String remarks,
            String isinCode
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pagination(
            Integer page,
            Integer totalItems,
            Integer totalPages,
            Boolean hasNextPage
    ) {}
}
