package tech.lougon.profitly.analysis.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Response of the legacy /api/quote/{symbol}?dividends=true endpoint.
 * Used as a fallback for units (SANB11, ENGI11...) that the v2 stocks/dividends
 * endpoint wrongly rejects as FIIs and the FII endpoint returns empty for.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BrapiLegacyDividendsResponse(List<Result> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Result(String symbol, DividendsData dividendsData) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DividendsData(List<BrapiDividendsResponse.CashDividend> cashDividends) {}
}
