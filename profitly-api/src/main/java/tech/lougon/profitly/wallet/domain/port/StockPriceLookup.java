package tech.lougon.profitly.wallet.domain.port;

import java.math.BigDecimal;
import java.util.Optional;

public interface StockPriceLookup {
    Optional<BigDecimal> findCurrentPrice(String ticker);
}
