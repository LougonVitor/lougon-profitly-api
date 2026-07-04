package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiFiiListResponse(
        @JsonAlias({"fiis", "results"}) List<FiiListItem> fiis,
        Pagination pagination
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FiiListItem(
            String symbol,
            String name,
            String cnpj,
            String mandate,
            String segmentoAtuacao,
            String tipoGestao,
            Double price,
            Double navPerShare,
            Double priceToNav,
            Double dividendYield12m,
            Number totalInvestors,
            String segmentType,
            String administratorName,
            String administratorCnpj
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pagination(
            Integer page,
            Integer totalItems,
            Integer totalPages,
            Boolean hasNextPage
    ) {}
}
