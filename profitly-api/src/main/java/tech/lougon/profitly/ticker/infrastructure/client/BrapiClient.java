package tech.lougon.profitly.ticker.infrastructure.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tech.lougon.profitly.ticker.infrastructure.client.dto.BrapiTickerResponse;

import java.util.ArrayList;
import java.util.List;

@Component
public class BrapiClient {

    private static final int PAGE_SIZE = 100;

    private final WebClient webClient;

    public BrapiClient(WebClient brapiWebClient) {
        this.webClient = brapiWebClient;
    }

    public List<BrapiTickerResponse.TickerItem> fetchAllTickers() {
        List<BrapiTickerResponse.TickerItem> all = new ArrayList<>();
        int page = 1;
        boolean hasNext = true;

        while (hasNext) {
            int p = page;
            BrapiTickerResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/tickers")
                            .queryParam("page", p)
                            .queryParam("limit", PAGE_SIZE)
                            .build())
                    .retrieve()
                    .bodyToMono(BrapiTickerResponse.class)
                    .block();

            if (response == null || response.results() == null) break;

            all.addAll(response.results());
            hasNext = response.pagination() != null && response.pagination().hasNextPage();
            page++;
        }

        return all;
    }
}
