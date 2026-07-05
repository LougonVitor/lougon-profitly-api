package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Response of /api/v2/funds/nav/history — root key is "history" and the list is FLAT
 * (one entry per symbol+date), unlike the nested treasury history shape.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiFundNavHistoryResponse(List<NavEntry> history, Pagination pagination) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NavEntry(
            String symbol,
            String cnpj,
            String date,
            String classOrSeries,
            Double totalAssets,
            Double navPerShare,
            Double equity,
            Double dailyApplications,
            Double dailyRedemptions,
            Number totalInvestors,
            Double monthlyReturn
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pagination(
            Integer page,
            Integer totalItems,
            Integer totalPages,
            Boolean hasNextPage
    ) {}
}
