package tech.lougon.profitly.stock.infrastructure.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tech.lougon.profitly.stock.infrastructure.client.dto.BrapiQuoteResponse;

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
}