package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record BrapiFiiListResponse(
        @JsonAlias({"fiis", "results"}) List<FiiListItem> fiis
) {
    public record FiiListItem(
            String symbol,
            String name,
            Double price,
            Double navPerShare,
            Double priceToNav,
            Double dividendYield12m,
            Number totalInvestors,
            String segmentType,
            String administratorName,
            String administratorCnpj
    ) {}
}
