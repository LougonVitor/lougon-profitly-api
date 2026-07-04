package tech.lougon.profitly.ticker.infrastructure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tech.lougon.profitly.ticker.infrastructure.client.dto.BrapiTickerResponse;

import java.util.ArrayList;
import java.util.List;

@Component
public class BrapiClient {

    private static final Logger log = LoggerFactory.getLogger(BrapiClient.class);

    /** /api/v2/tickers caps each response at 2000 items. */
    private static final int PAGE_SIZE = 2000;

    /**
     * SubTypes fetched from the general /api/v2/tickers endpoint.
     * FIIs, fiagro/fi-infra funds, treasury and crypto have dedicated list endpoints;
     * fidc and fip come from here because /api/v2/funds/list returns nothing for them.
     */
    private static final List<String> GENERAL_SUB_TYPES = List.of("stock", "unit", "bdr", "fidc", "fip");

    private final WebClient webClient;

    public BrapiClient(WebClient brapiWebClient) {
        this.webClient = brapiWebClient;
    }

    /** Fetches stock, unit and bdr tickers — one paginated request cycle per subType. */
    public List<BrapiTickerResponse.TickerItem> fetchAllTickers() {
        List<BrapiTickerResponse.TickerItem> all = new ArrayList<>();
        for (String subType : GENERAL_SUB_TYPES) {
            List<BrapiTickerResponse.TickerItem> items = fetchTickersBySubType(subType);
            log.info("Fetched {} tickers with subType={}", items.size(), subType);
            all.addAll(items);
        }
        return all;
    }

    public List<BrapiTickerResponse.TickerItem> fetchTickersBySubType(String subType) {
        List<BrapiTickerResponse.TickerItem> all = new ArrayList<>();
        int page = 1;
        boolean hasNext = true;

        while (hasNext) {
            int p = page;
            BrapiTickerResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/tickers")
                            .queryParam("subType", subType)
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
