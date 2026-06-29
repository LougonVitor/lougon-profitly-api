package tech.lougon.profitly.wallet.infrastructure.stock;

import org.springframework.stereotype.Component;
import tech.lougon.profitly.stock.domain.repository.StockRepository;
import tech.lougon.profitly.wallet.domain.port.StockMarketData;
import tech.lougon.profitly.wallet.domain.port.StockPriceLookup;

import java.util.Optional;

@Component
public class StockPriceLookupImpl implements StockPriceLookup {

    private final StockRepository stockRepository;

    public StockPriceLookupImpl(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public Optional<StockMarketData> findMarketData(String ticker) {
        return stockRepository.findByTicker(ticker)
                .map(quote -> new StockMarketData(
                        quote.getData().regularMarketPrice(),
                        quote.getData().logoUrl()
                ));
    }
}
