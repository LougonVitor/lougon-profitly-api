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
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFiiRawListResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiDividendsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFinancialDataResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiHistoricalResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiStatisticsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiTreasuryListResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiTreasuryIndicatorsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiTreasuryHistoryResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFundListResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFundIndicatorsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFundNavHistoryResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFundRawListResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiFundDividendsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiCryptoAvailableResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiCryptoResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiStockQuoteResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiStockProfileResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiStockStatementsResponse;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiLegacyDividendsResponse;

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

    /** Fetches current indicators for a single symbol. */
    public Optional<BrapiFiiIndicatorsResponse.FiiIndicator> fetchFiiIndicators(String symbol) {
        List<BrapiFiiIndicatorsResponse.FiiIndicator> list = fetchFiiIndicatorsBatch(symbol);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** Fetches current indicators for up to 20 comma-separated symbols (used by the scheduler). */
    public List<BrapiFiiIndicatorsResponse.FiiIndicator> fetchFiiIndicatorsBatch(String symbols) {
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

    /**
     * Batch variant: up to 20 comma-separated symbols in one call. Returns every entry
     * (all symbols mixed) — the caller groups by symbol. brapi caps at 20 symbols.
     */
    public List<BrapiFiiIndicatorsHistoryResponse.FiiHistoryEntry> fetchFiiIndicatorsHistoryBatch(
            String symbols, String startDate) {
        try {
            BrapiFiiIndicatorsHistoryResponse response = webClient.get()
                    .uri(u -> {
                        var b = u.path("/api/v2/fii/indicators/history")
                                .queryParam("symbols", symbols)
                                .queryParam("sortOrder", "asc");
                        if (startDate != null) b = b.queryParam("startDate", startDate);
                        return b.build();
                    })
                    .retrieve()
                    .bodyToMono(BrapiFiiIndicatorsHistoryResponse.class)
                    .block();

            if (response == null || response.history() == null) return List.of();
            return response.history().stream()
                    .filter(e -> e != null && e.symbol() != null && e.referenceDate() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch FII indicator history batch [{}]: {}", symbols, e.getMessage());
            return List.of();
        }
    }

    /**
     * Batch variant of the dividends fetch: up to 20 comma-separated symbols in one call.
     * Returns every payout (all symbols mixed) — the caller groups by symbol.
     */
    public List<BrapiFiiDividendsResponse.FiiDividend> fetchFiiDividendsBatch(String symbols, String startDate) {
        try {
            BrapiFiiDividendsResponse response = webClient.get()
                    .uri(u -> {
                        var b = u.path("/api/v2/fii/dividends")
                                .queryParam("symbols", symbols)
                                .queryParam("sortOrder", "desc");
                        if (startDate != null) b = b.queryParam("startDate", startDate);
                        return b.build();
                    })
                    .retrieve()
                    .bodyToMono(BrapiFiiDividendsResponse.class)
                    .block();

            if (response == null || response.dividends() == null) return List.of();
            return response.dividends().stream()
                    .filter(d -> d != null && d.symbol() != null && d.rate() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch FII dividends batch [{}]: {}", symbols, e.getMessage());
            return List.of();
        }
    }

    public List<BrapiTreasuryListResponse.TreasuryItem> fetchTreasuryList() {
        try {
            BrapiTreasuryListResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/treasury/list")
                            .queryParam("limit", 10000)
                            .build())
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

    /** Up to 20 symbols per call, comma-separated. Same item shape as /treasury/list. */
    public List<BrapiTreasuryListResponse.TreasuryItem> fetchTreasuryIndicators(String symbols) {
        try {
            BrapiTreasuryIndicatorsResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/treasury/indicators")
                            .queryParam("symbols", symbols)
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiTreasuryIndicatorsResponse.class)
                    .block();
            if (response == null || response.results() == null) return List.of();
            return response.results().stream().filter(t -> t != null && t.symbol() != null).toList();
        } catch (Exception e) {
            log.warn("Failed to fetch treasury indicators for [{}]: {}", symbols, e.getMessage());
            return List.of();
        }
    }

    /**
     * Daily rate/price series for up to 20 comma-separated symbols in one call.
     * Each result carries its own nested history keyed by baseDate.
     */
    public List<BrapiTreasuryHistoryResponse.TreasuryHistoryResult> fetchTreasuryHistory(
            String symbols, String startDate, String endDate) {
        try {
            BrapiTreasuryHistoryResponse response = webClient.get()
                    .uri(u -> {
                        var b = u.path("/api/v2/treasury/indicators/history")
                                .queryParam("symbols", symbols)
                                .queryParam("sortOrder", "asc");
                        if (startDate != null) b = b.queryParam("startDate", startDate);
                        if (endDate != null) b = b.queryParam("endDate", endDate);
                        return b.build();
                    })
                    .retrieve()
                    .bodyToMono(BrapiTreasuryHistoryResponse.class)
                    .block();
            if (response == null || response.results() == null) return List.of();
            return response.results().stream().filter(r -> r != null && r.symbol() != null).toList();
        } catch (Exception e) {
            log.warn("Failed to fetch treasury history for [{}]: {}", symbols, e.getMessage());
            return List.of();
        }
    }

    /**
     * Fetches all funds of a given assetType from /api/v2/funds/list in a single request.
     * assetType values: "fiagro" | "fiinfra" | "fidc" | "fip" | "fif" | "etf" | "other".
     */
    public List<BrapiFundListResponse.FundItem> fetchFundList(String assetType) {
        try {
            BrapiFundListResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/funds/list")
                            .queryParam("assetType", assetType)
                            .queryParam("limit", 10000)
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiFundListResponse.class)
                    .block();
            if (response == null || response.funds() == null) return List.of();
            return response.funds().stream().filter(f -> f != null && f.symbol() != null).toList();
        } catch (Exception e) {
            log.warn("Failed to fetch fund list for assetType={}: {}", assetType, e.getMessage());
            return List.of();
        }
    }

    /** Monthly indicators for up to 20 comma-separated fund symbols via /funds/indicators. */
    public List<BrapiFundIndicatorsResponse.FundIndicators> fetchFundIndicators(String symbols) {
        try {
            BrapiFundIndicatorsResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/funds/indicators")
                            .queryParam("symbols", symbols)
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiFundIndicatorsResponse.class)
                    .block();
            if (response == null || response.funds() == null) return List.of();
            return response.funds().stream().filter(f -> f != null && f.symbol() != null).toList();
        } catch (Exception e) {
            log.warn("Failed to fetch fund indicators for [{}]: {}", symbols, e.getMessage());
            return List.of();
        }
    }

    /**
     * Daily NAV series for up to 20 comma-separated fund symbols via /funds/nav/history.
     * The response is a FLAT list (one entry per symbol+date); pages are followed until
     * exhausted so callers always get the complete window.
     */
    public List<BrapiFundNavHistoryResponse.NavEntry> fetchFundNavHistory(
            String symbols, String startDate, String endDate) {
        List<BrapiFundNavHistoryResponse.NavEntry> all = new java.util.ArrayList<>();
        int page = 1;
        while (true) {
            final int currentPage = page;
            try {
                BrapiFundNavHistoryResponse response = webClient.get()
                        .uri(u -> {
                            var b = u.path("/api/v2/funds/nav/history")
                                    .queryParam("symbols", symbols)
                                    .queryParam("limit", 10000)
                                    .queryParam("sortOrder", "asc")
                                    .queryParam("page", currentPage);
                            if (startDate != null) b = b.queryParam("startDate", startDate);
                            if (endDate != null) b = b.queryParam("endDate", endDate);
                            return b.build();
                        })
                        .retrieve()
                        .bodyToMono(BrapiFundNavHistoryResponse.class)
                        .block();
                if (response == null || response.history() == null || response.history().isEmpty()) break;
                response.history().stream()
                        .filter(h -> h != null && h.symbol() != null && h.date() != null)
                        .forEach(all::add);
                if (response.pagination() == null || !Boolean.TRUE.equals(response.pagination().hasNextPage())) break;
                page++;
            } catch (Exception e) {
                log.warn("Failed to fetch fund NAV history for [{}] page {}: {}", symbols, currentPage, e.getMessage());
                break;
            }
        }
        return all;
    }

    /** Dividend events for up to 20 comma-separated fund symbols via /funds/dividends. */
    public List<BrapiFundDividendsResponse.FundDividend> fetchFundDividends(String symbols, String startDate) {
        try {
            BrapiFundDividendsResponse response = webClient.get()
                    .uri(u -> {
                        var b = u.path("/api/v2/funds/dividends")
                                .queryParam("symbols", symbols)
                                .queryParam("limit", 10000)
                                .queryParam("sortOrder", "desc");
                        if (startDate != null) b = b.queryParam("startDate", startDate);
                        return b.build();
                    })
                    .retrieve()
                    .bodyToMono(BrapiFundDividendsResponse.class)
                    .block();
            if (response == null || response.dividends() == null) return List.of();
            return response.dividends().stream()
                    .filter(d -> d != null && d.symbol() != null && d.rate() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch fund dividends for [{}]: {}", symbols, e.getMessage());
            return List.of();
        }
    }

    /**
     * Generic fetch for the fund document endpoints (profile, portfolio and the
     * fiagro/fidc/fip reports and portfolios). Rows come back as raw maps — callers
     * store them whole under the raw JSON pattern. {@code path} examples:
     * "/api/v2/funds/profile", "/api/v2/funds/fiagro/reports".
     */
    public List<java.util.Map<String, Object>> fetchFundDocuments(String path, String symbols) {
        try {
            BrapiFundRawListResponse response = webClient.get()
                    .uri(u -> u.path(path)
                            .queryParam("symbols", symbols)
                            .queryParam("limit", 10000)
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiFundRawListResponse.class)
                    .block();
            if (response == null || response.items() == null) return List.of();
            return response.items().stream().filter(i -> i != null && i.get("symbol") != null).toList();
        } catch (Exception e) {
            log.warn("Failed to fetch fund documents {} for [{}]: {}", path, symbols, e.getMessage());
            return List.of();
        }
    }

    // ── Stock batch endpoints (comma-separated symbols) ─────────────────────

    /** Fetches current quotes for comma-separated symbols via /api/v2/stocks/quote. */
    public List<BrapiStockQuoteResponse.Result> fetchStockQuotes(String symbols) {
        try {
            BrapiStockQuoteResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/stocks/quote").queryParam("symbols", symbols).build())
                    .retrieve()
                    .bodyToMono(BrapiStockQuoteResponse.class)
                    .block();
            if (response == null || response.results() == null) return List.of();
            return response.results().stream()
                    .filter(r -> r != null && r.symbol() != null && r.data() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch stock quotes for [{}]: {}", symbols, e.getMessage());
            return List.of();
        }
    }

    /** Fetches company profiles for comma-separated symbols via /api/v2/stocks/profile. */
    public List<BrapiStockProfileResponse.Result> fetchStockProfiles(String symbols) {
        try {
            BrapiStockProfileResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/stocks/profile").queryParam("symbols", symbols).build())
                    .retrieve()
                    .bodyToMono(BrapiStockProfileResponse.class)
                    .block();
            if (response == null || response.results() == null) return List.of();
            return response.results().stream()
                    .filter(r -> r != null && r.symbol() != null && r.data() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch stock profiles for [{}]: {}", symbols, e.getMessage());
            return List.of();
        }
    }

    /** Fetches current statistics for comma-separated symbols via /api/v2/stocks/statistics. */
    public List<BrapiStatisticsResponse.Result> fetchStatisticsBatch(String symbols) {
        try {
            BrapiStatisticsResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/stocks/statistics")
                            .queryParam("symbols", symbols)
                            .queryParam("mode", "current")
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiStatisticsResponse.class)
                    .block();
            if (response == null || response.results() == null) return List.of();
            return response.results().stream()
                    .filter(r -> r != null && r.symbol() != null && r.data() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch statistics for [{}]: {}", symbols, e.getMessage());
            return List.of();
        }
    }

    /** Fetches current financial data for comma-separated symbols via /api/v2/stocks/financial-data. */
    public List<BrapiFinancialDataResponse.Result> fetchFinancialDataBatch(String symbols) {
        try {
            BrapiFinancialDataResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/stocks/financial-data")
                            .queryParam("symbols", symbols)
                            .queryParam("mode", "current")
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiFinancialDataResponse.class)
                    .block();
            if (response == null || response.results() == null) return List.of();
            return response.results().stream()
                    .filter(r -> r != null && r.symbol() != null && r.data() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch financial data for [{}]: {}", symbols, e.getMessage());
            return List.of();
        }
    }

    /**
     * Fetches cash dividends for comma-separated symbols via /api/v2/stocks/dividends.
     * Some symbols make brapi return 400 for the whole batch — on failure the batch is
     * split in half and retried so one bad symbol doesn't discard the others.
     */
    public List<BrapiDividendsResponse.Result> fetchDividendsBatch(String symbols) {
        try {
            BrapiDividendsResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/stocks/dividends").queryParam("symbols", symbols).build())
                    .retrieve()
                    .bodyToMono(BrapiDividendsResponse.class)
                    .block();
            if (response == null || response.results() == null) return List.of();
            return response.results().stream()
                    .filter(r -> r != null && r.symbol() != null && r.data() != null)
                    .toList();
        } catch (Exception e) {
            if (symbols.contains(",")) {
                String[] parts = symbols.split(",");
                int mid = parts.length / 2;
                List<BrapiDividendsResponse.Result> out = new java.util.ArrayList<>();
                out.addAll(fetchDividendsBatch(String.join(",", java.util.Arrays.copyOfRange(parts, 0, mid))));
                out.addAll(fetchDividendsBatch(String.join(",", java.util.Arrays.copyOfRange(parts, mid, parts.length))));
                return out;
            }
            // brapi wrongly flags units ending in "11" (SANB11, ENGI11, BPAC11...) as FIIs
            // and returns 400 FII_DIVIDENDS_MISUSE — fall back to the FII dividends endpoint
            List<BrapiFiiDividendsResponse.FiiDividend> fiiDividends = fetchFiiDividends(symbols);
            if (!fiiDividends.isEmpty()) {
                List<BrapiDividendsResponse.CashDividend> cash = fiiDividends.stream()
                        .map(d -> new BrapiDividendsResponse.CashDividend(
                                null, d.paymentDate(), d.rate(), d.relatedTo(),
                                d.approvedOn(), d.isinCode(), d.label(), d.lastDatePrior(), d.remarks()))
                        .toList();
                return List.of(new BrapiDividendsResponse.Result(symbols, new BrapiDividendsResponse.Data(cash, List.of())));
            }
            // Units are not FIIs, so the FII endpoint comes back empty too. The legacy
            // /api/quote endpoint has no "ends with 11" heuristic and returns real data.
            List<BrapiDividendsResponse.CashDividend> legacy = fetchLegacyDividends(symbols);
            if (!legacy.isEmpty()) {
                return List.of(new BrapiDividendsResponse.Result(symbols, new BrapiDividendsResponse.Data(legacy, List.of())));
            }
            log.warn("Failed to fetch dividends for [{}]: {}", symbols, e.getMessage());
            return List.of();
        }
    }

    /**
     * Legacy v1 endpoint: /api/quote/{symbol}?dividends=true — single symbol only.
     * Unlike /v2/funds/dividends (last 12 months only), it carries the FULL payout
     * history, so the fund sync uses it to deepen first-time backfills.
     */
    public List<BrapiDividendsResponse.CashDividend> fetchLegacyDividends(String symbol) {
        try {
            BrapiLegacyDividendsResponse response = webClient.get()
                    .uri(u -> u.path("/api/quote/" + symbol)
                            .queryParam("dividends", "true")
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiLegacyDividendsResponse.class)
                    .block();
            if (response == null || response.results() == null || response.results().isEmpty()) return List.of();
            var data = response.results().get(0).dividendsData();
            if (data == null || data.cashDividends() == null) return List.of();
            return data.cashDividends().stream()
                    .filter(d -> d != null && d.rate() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Legacy dividends fallback failed for {}: {}", symbol, e.getMessage());
            return List.of();
        }
    }

    /**
     * Fetches financial statements for comma-separated symbols.
     * endpoint: "balance-sheet" | "income-statement" | "cash-flow" | "value-added";
     * period: "annual" | "quarterly" (quarterly rows are per-quarter values).
     * Rows come back as generic maps so every field is preserved.
     */
    public List<BrapiStockStatementsResponse.Result> fetchStatements(String endpoint, String symbols, String period) {
        try {
            BrapiStockStatementsResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/stocks/" + endpoint)
                            .queryParam("symbols", symbols)
                            .queryParam("period", period)
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiStockStatementsResponse.class)
                    .block();
            if (response == null || response.results() == null) return List.of();
            return response.results().stream()
                    .filter(r -> r != null && r.symbol() != null && r.data() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch {} ({}) for [{}]: {}", endpoint, period, symbols, e.getMessage());
            return List.of();
        }
    }

    /**
     * Fetches yearly indicator history for comma-separated symbols.
     * endpoint: "statistics" | "financial-data" (mode=history returns one row per year,
     * same generic shape as the statement endpoints).
     */
    public List<BrapiStockStatementsResponse.Result> fetchIndicatorHistory(String endpoint, String symbols) {
        try {
            BrapiStockStatementsResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/stocks/" + endpoint)
                            .queryParam("symbols", symbols)
                            .queryParam("mode", "history")
                            .queryParam("period", "annual")
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiStockStatementsResponse.class)
                    .block();
            if (response == null || response.results() == null) return List.of();
            return response.results().stream()
                    .filter(r -> r != null && r.symbol() != null && r.data() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch {} history for [{}]: {}", endpoint, symbols, e.getMessage());
            return List.of();
        }
    }

    /** Fetches all available coin symbols from /api/v2/crypto/available. */
    public List<String> fetchCryptoAvailable() {
        try {
            BrapiCryptoAvailableResponse response = webClient.get()
                    .uri("/api/v2/crypto/available")
                    .retrieve()
                    .bodyToMono(BrapiCryptoAvailableResponse.class)
                    .block();
            if (response == null || response.coins() == null) return List.of();
            return response.coins().stream().filter(c -> c != null && !c.isBlank()).toList();
        } catch (Exception e) {
            log.warn("Failed to fetch available crypto coins: {}", e.getMessage());
            return List.of();
        }
    }

    /** Fetches BRL quotes for comma-separated coin symbols via /api/v2/crypto. */
    public List<BrapiCryptoResponse.CryptoQuote> fetchCryptoQuotes(String coins) {
        return fetchCryptoQuotes(coins, null, null);
    }

    /** Fetches BRL quotes with optional daily price history (range=max|1mo|..., interval=1d). */
    public List<BrapiCryptoResponse.CryptoQuote> fetchCryptoQuotes(String coins, String range, String interval) {
        return fetchCryptoQuotes(coins, range, interval, "BRL");
    }

    /** Fetches quotes in the given currency (BRL/USD) with optional daily price history. */
    public List<BrapiCryptoResponse.CryptoQuote> fetchCryptoQuotes(String coins, String range,
                                                                    String interval, String currency) {
        try {
            BrapiCryptoResponse response = webClient.get()
                    .uri(u -> {
                        u.path("/api/v2/crypto")
                                .queryParam("coin", coins)
                                .queryParam("currency", currency);
                        if (range != null) u.queryParam("range", range);
                        if (interval != null) u.queryParam("interval", interval);
                        return u.build();
                    })
                    .retrieve()
                    .bodyToMono(BrapiCryptoResponse.class)
                    .block();
            if (response == null || response.coins() == null) return List.of();
            return response.coins().stream().filter(c -> c != null && c.coin() != null).toList();
        } catch (Exception e) {
            log.warn("Failed to fetch crypto quotes for [{}] in {}: {}", coins, currency, e.getMessage());
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

    /**
     * Generic fetch for the FII document endpoints (properties, properties/history,
     * portfolio, portfolio/history). Rows come back as raw maps — callers store them
     * whole under the raw JSON pattern. {@code path} examples: "/api/v2/fii/properties",
     * "/api/v2/fii/portfolio/history".
     */
    public List<java.util.Map<String, Object>> fetchFiiDocuments(String path, String symbols) {
        try {
            BrapiFiiRawListResponse response = webClient.get()
                    .uri(u -> u.path(path)
                            .queryParam("symbols", symbols)
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiFiiRawListResponse.class)
                    .block();
            if (response == null || response.items() == null) return List.of();
            return response.items().stream().filter(i -> i != null && i.get("symbol") != null).toList();
        } catch (Exception e) {
            log.warn("Failed to fetch FII documents {} for [{}]: {}", path, symbols, e.getMessage());
            return List.of();
        }
    }
}
