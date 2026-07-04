package tech.lougon.profitly.analysis.infrastructure.client.dto;

import java.util.List;

public record BrapiFiiHistoricalResponse(List<FiiHistoricalResult> fiis) {

    public record FiiHistoricalResult(String symbol, List<PriceBar> historicalDataPrice) {}

    public record PriceBar(
            Long date,
            Double open,
            Double high,
            Double low,
            Double close,
            Long volume,
            Double adjustedClose
    ) {}
}
