package tech.lougon.profitly.analysis.infrastructure.client.dto;

import java.util.List;

public record BrapiFiiDividendsResponse(List<FiiDividend> dividends) {

    public record FiiDividend(
            String symbol,
            String approvedOn,
            String label,
            String lastDatePrior,
            String paymentDate,
            Double rate,
            String relatedTo,
            String isinCode,
            String remarks
    ) {}
}
