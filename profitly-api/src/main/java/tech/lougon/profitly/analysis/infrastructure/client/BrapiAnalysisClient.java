package tech.lougon.profitly.analysis.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiDividendsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiHistoricalResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiIndicatorsHistoryResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiIndicatorsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiListResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiDividendsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFinancialDataResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiHistoricalResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiStatisticsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiTreasuryListResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiTreasuryIndicatorsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiTreasuryHistoryResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFundListResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFundDividendsResponse;

import java.util.List;
import java.util.Optional;

@Component
public class BrapiAnalysisClient {

    private static final Logger log = LoggerFactory.getLogger(BrapiAnalysisClient.class);

    private final WebClient webClient;

    public BrapiAnalysisClient(WebClient brapiWebClient) {
        this.webClient = brapiWebClient;
    }

    public Optional<BrapiStatisticsResponse.Data> fetchStatistics(String symbol) {
        try {
            BrapiStatisticsResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/stocks/statistics")
                            .queryParam("symbols", symbol)
                            .queryParam("mode", "current")
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiStatisticsResponse.class)
                    .block();

            if (response == null || response.results() == null || response.results().isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(response.results().get(0).data());
        } catch (Exception e) {
            log.warn("Failed to fetch statistics for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<BrapiFinancialDataResponse.Data> fetchFinancialData(String symbol) {
        try {
            BrapiFinancialDataResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/stocks/financial-data")
                            .queryParam("symbols", symbol)
                            .queryParam("mode", "current")
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiFinancialDataResponse.class)
                    .block();

            if (response == null || response.results() == null || response.results().isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable(response.results().get(0).data());
        } catch (Exception e) {
            log.warn("Failed to fetch financial-data for {}: {}", symbol, e.getMessage());
            return Optional.empty();
        }
    }

    /** Fetches FII daily OHLCV history from /api/v2/fii/historical. */
    public List<BrapiFiiHistoricalResponse.PriceBar> fetchFiiHistory(String symbol, String range) {
        try {
            BrapiFiiHistoricalResponse response = webClient.get()
                    .uri(u -> {
                        var b = u.path("/api/v2/fii/historical")
                                .queryParam("symbols", symbol);
                        if (range != null && !range.equals("max")) b = b.queryParam("range", range);
                        return b.build();
                    })
                    .retrieve()
                    .bodyToMono(BrapiFiiHistoricalResponse.class)
                    .block();

            if (response == null || response.fiis() == null || response.fiis().isEmpty()) {
                return List.of();
            }
            var bars = response.fiis().get(0).historicalDataPrice();
            return bars != null ? bars : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch FII history for {}: {}", symbol, e.getMessage());
            return List.of();
        }
    }

    public List<BrapiHistoricalResponse.PriceBar> fetchHistory(String symbol, String range) {
        return fetchHistoryWithInterval(symbol, range, "1d");
    }

    public List<BrapiHistoricalResponse.PriceBar> fetchHistoryWithInterval(String symbol, String range, String interval) {
        try {
            BrapiHistoricalResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/stocks/historical")
                            .queryParam("symbols", symbol)
                            .queryParam("range", range)
                            .queryParam("interval", interval)
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiHistoricalResponse.class)
                    .block();

            if (response == null || response.results() == null || response.results().isEmpty()) {
                return List.of();
            }
            var data = response.results().get(0).data();
            if (data == null || data.historicalDataPrice() == null) return List.of();
            return data.historicalDataPrice();
        } catch (Exception e) {
            log.warn("Failed to fetch history for {}: {}", symbol, e.getMessage());
            return List.of();
        }
    }

    public List<BrapiDividendsResponse.CashDividend> fetchDividends(String symbol) {
        try {
            BrapiDividendsResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/stocks/dividends")
                            .queryParam("symbols", symbol)
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiDividendsResponse.class)
                    .block();

            if (response == null || response.results() == null || response.results().isEmpty()) {
                return List.of();
            }
            var data = response.results().get(0).data();
            if (data == null || data.cashDividends() == null) return List.of();
            return data.cashDividends();
        } catch (Exception e) {
            log.warn("Failed to fetch dividends for {}: {}", symbol, e.getMessage());
            return List.of();
        }
    }

    /**
     * Fetches all FIIs from /api/v2/fii/list in a single request.
     * The endpoint defaults to 20 items per page but accepts arbitrarily high limits.
     */
    public List<BrapiFiiListResponse.FiiListItem> fetchFiiList() {
        try {
            BrapiFiiListResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/fii/list")
                            .queryParam("limit", 10000)
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiFiiListResponse.class)
                    .block();

            if (response == null || response.fiis() == null) return List.of();
            return response.fiis().stream()
                    .filter(f -> f != null && f.symbol() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch FII list: {}", e.getMessage());
            return List.of();
        }
    }

    /** Fetches current indicators for a single symbol (used by the API controller). */
    public Optional<BrapiFiiIndicatorsResponse.FiiIndicatorWithInfo> fetchFiiIndicators(String symbol) {
        List<BrapiFiiIndicatorsResponse.FiiIndicatorWithInfo> list = fetchFiiIndicatorsBatch(symbol);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Fetches current indicators for up to 20 comma-separated symbols (used by the scheduler). */
    public List<BrapiFiiIndicatorsResponse.FiiIndicatorWithInfo> fetchFiiIndicatorsBatch(String symbols) {
        try {
            BrapiFiiIndicatorsResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/fii/indicators")
                            .queryParam("symbols", symbols)
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiFiiIndicatorsResponse.class)
                    .block();

            if (response == null || response.fiis() == null) return List.of();
            return response.fiis().stream()
                    .filter(f -> f != null && f.symbol() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch FII indicators for [{}]: {}", symbols, e.getMessage());
            return List.of();
        }
    }

    public List<BrapiFiiIndicatorsHistoryResponse.FiiHistoryEntry> fetchFiiIndicatorsHistory(
            String symbol, String startDate, String endDate) {
        try {
            BrapiFiiIndicatorsHistoryResponse response = webClient.get()
                    .uri(u -> {
                        var b = u.path("/api/v2/fii/indicators/history")
                                .queryParam("symbols", symbol);
                        if (startDate != null) b = b.queryParam("startDate", startDate);
                        if (endDate   != null) b = b.queryParam("endDate",   endDate);
                        return b.build();
                    })
                    .retrieve()
                    .bodyToMono(BrapiFiiIndicatorsHistoryResponse.class)
                    .block();

            if (response == null || response.history() == null) return List.of();
            return response.history().stream()
                    .filter(e -> symbol.equalsIgnoreCase(e.symbol()))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch FII indicator history for {}: {}", symbol, e.getMessage());
            return List.of();
        }
    }

    public List<BrapiTreasuryListResponse.TreasuryItem> fetchTreasuryList() {
        try {
            BrapiTreasuryListResponse response = webClient.get()
                    .uri("/api/v2/treasury/list")
                    .retrieve()
                    .bodyToMono(BrapiTreasuryListResponse.class)
                    .block();
            if (response == null || response.results() == null) return List.of();
            return response.results().stream().filter(t -> t != null && t.symbol() != null).toList();
        } catch (Exception e) {
            log.warn("Failed to fetch treasury list: {}", e.getMessage());
            return List.of();
        }
    }

    public List<BrapiTreasuryIndicatorsResponse.TreasuryIndicator> fetchTreasuryIndicators(String symbols) {
        try {
            BrapiTreasuryIndicatorsResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/treasury/indicators")
                            .queryParam("symbols", symbols)
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiTreasuryIndicatorsResponse.class)
                    .block();
            if (response == null || response.treasuries() == null) return List.of();
            return response.treasuries().stream().filter(t -> t != null && t.symbol() != null).toList();
        } catch (Exception e) {
            log.warn("Failed to fetch treasury indicators for [{}]: {}", symbols, e.getMessage());
            return List.of();
        }
    }

    public List<BrapiTreasuryHistoryResponse.TreasuryHistoryEntry> fetchTreasuryHistory(
            String symbol, String startDate, String endDate) {
        try {
            BrapiTreasuryHistoryResponse response = webClient.get()
                    .uri(u -> {
                        var b = u.path("/api/v2/treasury/indicators/history")
                                .queryParam("symbols", symbol);
                        if (startDate != null) b = b.queryParam("startDate", startDate);
                        if (endDate != null) b = b.queryParam("endDate", endDate);
                        return b.build();
                    })
                    .retrieve()
                    .bodyToMono(BrapiTreasuryHistoryResponse.class)
                    .block();
            if (response == null || response.results() == null) return List.of();
            return response.results().stream().filter(e -> e != null && e.referenceDate() != null).toList();
        } catch (Exception e) {
            log.warn("Failed to fetch treasury history for {}: {}", symbol, e.getMessage());
            return List.of();
        }
    }

    /** Fetches FIAGRO funds from brapi. Note: brapi uses /api/v2/fiagro/list (same pattern as /fii/list). */
    public List<BrapiFundListResponse.FundItem> fetchFundList(String symbols) {
        try {
            BrapiFundListResponse response = webClient.get()
                    .uri(u -> {
                        var b = u.path("/api/v2/fiagro/list");
                        if (symbols != null && !symbols.isBlank()) b = b.queryParam("symbols", symbols);
                        return b.build();
                    })
                    .retrieve()
                    .bodyToMono(BrapiFundListResponse.class)
                    .block();
            if (response == null || response.results() == null) return List.of();
            return response.results().stream().filter(f -> f != null && f.symbol() != null).toList();
        } catch (Exception e) {
            log.warn("Failed to fetch fund list: {}", e.getMessage());
            return List.of();
        }
    }

    public List<BrapiFundDividendsResponse.FundDividend> fetchFundDividends(String symbol) {
        try {
            BrapiFundDividendsResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/fiagro/dividends")
                            .queryParam("symbols", symbol)
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiFundDividendsResponse.class)
                    .block();
            if (response == null || response.dividends() == null) return List.of();
            return response.dividends().stream()
                    .filter(d -> d != null && symbol.equalsIgnoreCase(d.symbol()))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch fund dividends for {}: {}", symbol, e.getMessage());
            return List.of();
        }
    }

    public List<BrapiFiiDividendsResponse.FiiDividend> fetchFiiDividends(String symbol) {
        try {
            BrapiFiiDividendsResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/fii/dividends")
                            .queryParam("symbols", symbol)
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiFiiDividendsResponse.class)
                    .block();

            if (response == null || response.dividends() == null) return List.of();
            return response.dividends().stream()
                    .filter(d -> d != null && symbol.equalsIgnoreCase(d.symbol()))
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch FII dividends for {}: {}", symbol, e.getMessage());
            return List.of();
        }
    }
}
