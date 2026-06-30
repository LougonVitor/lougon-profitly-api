package tech.lougon.profitly.wallet.infrastructure.stock;

import org.springframework.stereotype.Component;
import tech.lougon.profitly.ticker.domain.repository.TickerRepository;
import tech.lougon.profitly.wallet.domain.port.StockMarketData;
import tech.lougon.profitly.wallet.domain.port.StockPriceLookup;

import java.util.Optional;

@Component
public class StockPriceLookupImpl implements StockPriceLookup {

    private final TickerRepository tickerRepository;

    public StockPriceLookupImpl(TickerRepository tickerRepository) {
        this.tickerRepository = tickerRepository;
    }

    @Override
    public Optional<StockMarketData> findMarketData(String symbol) {
        return tickerRepository.findBySymbol(symbol)
                .map(t -> new StockMarketData(t.lastPrice(), t.logoUrl(), t.subType()));
    }
}
