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
     * {@code startDate} omitted returns the most recent observations first (desc), which is
     * what a one-shot backfill needs since brapi caps each series at {@code limit} rows —
     * requesting from the earliest date would truncate before reaching today.
     */
    public List<BrapiMacroResponse.SeriesResult> fetchSeries(String symbols, String startDate, int limit) {
        try {
            BrapiMacroResponse response = webClient.get()
                    .uri(u -> {
                        var b = u.path("/api/v2/macro")
                                .queryParam("symbols", symbols)
                                .queryParam("limit", limit);
                        if (startDate != null) b = b.queryParam("startDate", startDate);
                        return b.build();
                    })
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
