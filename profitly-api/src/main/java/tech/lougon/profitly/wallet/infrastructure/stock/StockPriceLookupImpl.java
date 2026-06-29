package tech.lougon.profitly.wallet.infrastructure.stock;

import org.springframework.stereotype.Component;
import tech.lougon.profitly.stock.domain.repository.StockRepository;
import tech.lougon.profitly.wallet.domain.port.StockPriceLookup;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class StockPriceLookupImpl implements StockPriceLookup {

    private final StockRepository stockRepository;

    public StockPriceLookupImpl(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public Optional<BigDecimal> findCurrentPrice(String ticker) {
        return stockRepository.findByTicker(ticker)
                .map(quote -> quote.getData().regularMarketPrice());
    }
}
