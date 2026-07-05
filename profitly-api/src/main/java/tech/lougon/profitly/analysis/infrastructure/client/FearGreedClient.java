package tech.lougon.profitly.analysis.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tech.lougon.profitly.analysis.infrastructure.client.dto.AlternativeMeFngResponse;

import java.util.List;

/**
 * Crypto Fear & Greed Index from api.alternative.me (free, no key).
 * Only called by schedulers — never during a user request.
 */
@Component
public class FearGreedClient {

    private static final Logger log = LoggerFactory.getLogger(FearGreedClient.class);

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.alternative.me")
            .build();

    /** Fetches the last {@code limit} daily readings; limit=0 returns the full history. */
    public List<AlternativeMeFngResponse.FngEntry> fetchIndex(int limit) {
        try {
            AlternativeMeFngResponse response = webClient.get()
                    .uri(u -> u.path("/fng/").queryParam("limit", limit).build())
                    .retrieve()
                    .bodyToMono(AlternativeMeFngResponse.class)
                    .block();
            if (response == null || response.data() == null) return List.of();
            return response.data().stream()
                    .filter(e -> e != null && e.value() != null && e.timestamp() != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch Fear & Greed index: {}", e.getMessage());
            return List.of();
        }
    }
}
