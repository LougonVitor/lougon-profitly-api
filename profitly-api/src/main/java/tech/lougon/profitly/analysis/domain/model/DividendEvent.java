package tech.lougon.profitly.analysis.domain.model;

public record DividendEvent(
        String symbol,
        String assetIssued,
        String paymentDate,
        Double rate,
        String relatedTo,
        String approvedOn,
        String label,
        String lastDatePrior,
        String remarks
) {}
