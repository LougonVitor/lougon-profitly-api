package tech.lougon.profitly.finance.domain.port;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bridges the finance module to how much money the user actually invested (wallet buys).
 * Implemented in infrastructure against the wallet module so finance depends only on this port.
 */
public interface InvestedLookup {

    /** Total amount invested (buys: quantity × paid price) by the user on or after {@code since}. */
    BigDecimal investedSince(String userId, LocalDate since);
}
