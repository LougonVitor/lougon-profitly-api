package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response of /api/v2/fii/indicators. The real payload is FLAT — every field sits
 * at the top level of each item (not nested under data/administrator). Carries the
 * richer monthly indicators (equity, totalAssets, sharesOutstanding, dividendYield1m,
 * monthlyReturn, asOfDate) that /fii/list does not include.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiFiiIndicatorsResponse(List<FiiIndicator> fiis) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FiiIndicator(
            String symbol,
            String asOfDate,
            Double price,
            Double navPerShare,
            Double priceToNav,
            /** Trailing 12-month dividend yield as a decimal fraction (0.12 = 12%). */
            Double dividendYield12m,
            /** Last-month dividend yield as a decimal fraction. */
            Double dividendYield1m,
            Double monthlyReturn,
            Long totalInvestors,
            Long sharesOutstanding,
            Double equity,
            Double totalAssets,
            String segmentType,
            String name,
            String cnpj,
            String mandate,
            String segmentoAtuacao,
            String tipoGestao,
            String administratorName,
            String administratorCnpj
    ) {}
}
