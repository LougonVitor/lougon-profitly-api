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
                .map(t -> new StockMarketData(t.lastPrice(), t.logoUrl(), resolveType(t),
                        t.longName() != null ? t.longName() : t.name()))
                .or(() -> findCrypto(symbol));
    }

    /**
     * Treasury keeps the indexer (ipca/selic) and FII keeps the segment (tijolo/papel)
     * in sub_type — group those by asset_type instead. Spelling variants in tickers
     * (fiagro/fi-agro, fiinfra/fi-infra) collapse to one canonical value so the
     * frontend maps a single label/icon per type.
     */
    private String resolveType(tech.lougon.profitly.ticker.domain.model.Ticker t) {
        if ("treasury".equalsIgnoreCase(t.assetType())) return "treasury";
        if ("fii".equalsIgnoreCase(t.assetType())) return "fii";
        String raw = t.subType() != null ? t.subType() : t.assetType();
        if (raw == null) return null;
        return switch (raw.toLowerCase()) {
            case "fiagro" -> "fi-agro";
            case "fiinfra" -> "fi-infra";
            default -> raw.toLowerCase();
        };
    }

    private Optional<StockMarketData> findCrypto(String symbol) {
        return cryptoQuoteRepository.findById(symbol.toUpperCase())
                .filter(q -> q.getPrice() != null)
                .map(q -> new StockMarketData(
                        BigDecimal.valueOf(q.getPrice()),
                        q.getImageUrl(),
                        "crypto",
                        q.getCoinName()));
    }
}
