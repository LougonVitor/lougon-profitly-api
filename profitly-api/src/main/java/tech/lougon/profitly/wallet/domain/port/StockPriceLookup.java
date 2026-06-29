package tech.lougon.profitly.wallet.domain.port;

import java.util.Optional;

public interface StockPriceLookup {
    Optional<StockMarketData> findMarketData(String ticker);
}
