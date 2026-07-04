package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Response of /api/v2/crypto?coin=BTC,ETH&currency=BRL. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiCryptoResponse(List<CryptoQuote> coins) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CryptoQuote(
            String coin,
            String coinName,
            String currency,
            String coinImageUrl,
            Double regularMarketPrice,
            Double regularMarketChangePercent,
            Double regularMarketDayHigh,
            Double regularMarketDayLow,
            Double regularMarketVolume,
            Double marketCap,
            Long regularMarketTime
    ) {}
}
