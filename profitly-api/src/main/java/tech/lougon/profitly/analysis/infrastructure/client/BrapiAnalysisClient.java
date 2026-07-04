package tech.lougon.profitly.analysis.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiIndicatorsHistoryResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiIndicatorsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiListResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiDividendsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFinancialDataResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiHistoricalResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiStatisticsResponse;

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

    /** Fetches FII list data for up to 20 comma-separated symbols via /api/v2/fii/list. */
    public List<BrapiFiiListResponse.FiiListItem> fetchFiiList(String symbols) {
        try {
            BrapiFiiListResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/fii/list")
                            .queryParam("symbols", symbols)
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiFiiListResponse.class)
                    .block();

            if (response == null || response.fiis() == null) return List.of();
            return response.fiis().stream()
                    .filter(f -> f != null && f.symbol() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch FII list for [{}]: {}", symbols, e.getMessage());
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

    public List<BrapiDividendsResponse.CashDividend> fetchFiiDividends(String symbol) {
        try {
            BrapiDividendsResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/fii/dividends")
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
            log.warn("Failed to fetch FII dividends for {}: {}", symbol, e.getMessage());
            return List.of();
        }
    }
}
