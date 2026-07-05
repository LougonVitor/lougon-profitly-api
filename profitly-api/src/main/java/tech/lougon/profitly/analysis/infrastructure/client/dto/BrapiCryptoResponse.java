package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Response of /api/v2/crypto?coin=BTC,ETH&currency=BRL[&range=max&interval=1d]. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiCryptoResponse(List<CryptoQuote> coins) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CryptoQuote(
            String coin,
            String coinName,
            String currency,
            Double currencyRateFromUSD,
            String coinImageUrl,
            Double regularMarketPrice,
            Double regularMarketChange,
            Double regularMarketChangePercent,
            Double regularMarketDayHigh,
            Double regularMarketDayLow,
            Double regularMarketVolume,
            Double marketCap,
            String regularMarketTime,
            List<HistoricalPrice> historicalDataPrice
    ) {}

    /** OHLC bar from historicalDataPrice; volume comes in coin units (fractional). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HistoricalPrice(
            Long date,
            Double open,
            Double high,
            Double low,
            Double close,
            Double volume,
            Double adjustedClose
    ) {}
}
