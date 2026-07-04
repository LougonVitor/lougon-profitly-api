package tech.lougon.profitly.analysis.infrastructure.client.dto;

import java.util.List;

public record BrapiFiiListResponse(List<FiiListItem> fiis) {

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
