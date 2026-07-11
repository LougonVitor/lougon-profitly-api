package tech.lougon.profitly.wallet.infrastructure.stock;

import org.springframework.stereotype.Component;
import tech.lougon.profitly.analysis.infrastructure.persistence.JpaCryptoQuoteRepository;
import tech.lougon.profitly.ticker.domain.repository.TickerRepository;
import tech.lougon.profitly.wallet.domain.port.StockMarketData;
import tech.lougon.profitly.wallet.domain.port.StockPriceLookup;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class StockPriceLookupImpl implements StockPriceLookup {

    private final TickerRepository tickerRepository;
    private final JpaCryptoQuoteRepository cryptoQuoteRepository;

    public StockPriceLookupImpl(TickerRepository tickerRepository,
                                JpaCryptoQuoteRepository cryptoQuoteRepository) {
        this.tickerRepository = tickerRepository;
        this.cryptoQuoteRepository = cryptoQuoteRepository;
    }

    @Override
    public Optional<StockMarketData> findMarketData(String symbol) {
        // Treasury symbols are stored lowercase, everything else uppercase — try both.
        return tickerRepository.findBySymbol(symbol)
                .or(() -> tickerRepository.findBySymbol(symbol.toUpperCase()))
                .or(() -> tickerRepository.findBySymbol(symbol.toLowerCase()))
                .map(t -> new StockMarketData(t.lastPrice(), t.logoUrl(), resolveType(t)))
                .or(() -> findCrypto(symbol));
    }

    /** Treasury tickers keep the indexer (ipca/selic) in sub_type — group them as "treasury". */
    private String resolveType(tech.lougon.profitly.ticker.domain.model.Ticker t) {
        if ("treasury".equalsIgnoreCase(t.assetType())) return "treasury";
        return t.subType() != null ? t.subType() : t.assetType();
    }

    private Optional<StockMarketData> findCrypto(String symbol) {
        return cryptoQuoteRepository.findById(symbol.toUpperCase())
                .filter(q -> q.getPrice() != null)
                .map(q -> new StockMarketData(
                        BigDecimal.valueOf(q.getPrice()),
                        q.getImageUrl(),
                        "crypto"));
    }
}
