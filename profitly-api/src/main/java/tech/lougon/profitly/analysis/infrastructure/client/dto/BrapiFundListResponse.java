package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Response of /api/v2/funds/list — root key is "funds". */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiFundListResponse(
        @JsonAlias({"funds", "results"}) List<FundItem> funds,
        Pagination pagination
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FundItem(
            String symbol,
            String cnpj,
            String name,
            String legalName,
            String assetType,
            String b3Classification,
            String administratorName,
            String administratorCnpj,
            String managerName,
            String managerCnpj,
            String status,
            Double price,
            Double navPerShare,
            Double priceToNav,
            Double equity,
            Double totalAssets,
            Number totalInvestors
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pagination(
            Integer page,
            Integer totalItems,
            Integer totalPages,
            Boolean hasNextPage
    ) {}
}
