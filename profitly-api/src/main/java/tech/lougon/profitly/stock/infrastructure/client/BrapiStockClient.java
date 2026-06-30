package tech.lougon.profitly.stock.infrastructure.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tech.lougon.profitly.stock.infrastructure.client.dto.BrapiFiiListResponse;
import tech.lougon.profitly.stock.infrastructure.client.dto.BrapiQuoteResponse;
import tech.lougon.profitly.stock.infrastructure.client.dto.BrapiTickerListResponse;

import java.util.ArrayList;
import java.util.List;

@Component
public class BrapiStockClient {

    private final WebClient webClient;

    public BrapiStockClient(WebClient brapiWebClient) {
        this.webClient = brapiWebClient;
    }

    public BrapiQuoteResponse fetchQuote(String ticker) {
        return webClient.get().uri("/api/v2/stocks/quote?symbols={ticker}", ticker)
                .retrieve()
                .bodyToMono(BrapiQuoteResponse.class)
                .block();
    }

    public BrapiQuoteResponse fetchQuotes(List<String> tickers) {
        String symbols = String.join(",", tickers);

        return webClient.get().uri("/api/v2/stocks/quote?symbols={symbols}", symbols)
                .retrieve()
                .bodyToMono(BrapiQuoteResponse.class)
                .block();
    }

    public BrapiTickerListResponse fetchTickerList() {
        return webClient.get().uri("/api/quote/list")
                .retrieve()
                .bodyToMono(BrapiTickerListResponse.class)
                .block();
    }

    public List<String> fetchAllFiiSymbols() {
        List<String> symbols = new ArrayList<>();
        int page = 1;
        boolean hasNext = true;

        while (hasNext) {
            int p = page;
            BrapiFiiListResponse response = webClient.get()
                    .uri(u -> u.path("/api/v2/fii/list").queryParam("page", p).queryParam("limit", 100).build())
                    .retrieve()
                    .bodyToMono(BrapiFiiListResponse.class)
                    .block();

            if (response == null || response.fiis() == null) break;

            response.fiis().forEach(f -> symbols.add(f.symbol()));
            hasNext = response.pagination() != null && response.pagination().hasNextPage();
            page++;
        }

        return symbols;
    }
}
