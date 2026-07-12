package tech.lougon.profitly.analysis.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tech.lougon.profitly.analysis.infrastructure.client.dto.BrapiMacroResponse;

import java.util.List;

/**
 * Fetches CDI/Selic/IPCA daily-index series from brapi's macro endpoint — used to compute
 * fixed-income (renda fixa) accrual locally. Only ever called from a scheduler, never per
 * user request.
 */
@Component
public class BrapiMacroClient {

    private static final Logger log = LoggerFactory.getLogger(BrapiMacroClient.class);

    private final WebClient webClient;

    public BrapiMacroClient(WebClient brapiWebClient) {
        this.webClient = brapiWebClient;
    }

    /**
     * {@code startDate} is required, not optional: brapi defaults it to "12 months ago"
     * when omitted, which silently truncates a backfill to the last year. sortOrder=desc
     * combined with an old startDate returns the most recent {@code limit} observations
     * within that window — the caller must pass a startDate old enough (e.g. the series'
     * documented start) that "most recent {@code limit}" still reaches today.
     */
    public List<BrapiMacroResponse.SeriesResult> fetchSeries(String symbols, String startDate, int limit) {
        try {
            BrapiMacroResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/macro")
                            .queryParam("symbols", symbols)
                            .queryParam("startDate", startDate)
                            .queryParam("sortOrder", "desc")
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiMacroResponse.class)
                    .block();

            if (response == null || response.results() == null) return List.of();
            return response.results();
        } catch (Exception e) {
            log.warn("Failed to fetch macro series [{}]: {}", symbols, e.getMessage());
            return List.of();
        }
    }
}
