package tech.lougon.profitly.wallet.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Renda-fixa contract terms, embedded (all-nullable columns) into wallet_positions.
 * Hibernate maps an all-null set of columns back to a null {@code FixedIncomeDetailsEmbeddable}
 * on read, so ticker-based positions never carry an empty instance of this.
 */
@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FixedIncomeDetailsEmbeddable {

    @Column(name = "fi_issuer")
    private String issuer;

    @Column(name = "fi_instrument_type", length = 10)
    private String instrumentType;

    @Column(name = "fi_indexer", length = 20)
    private String indexer;

    @Column(name = "fi_rate_percent", precision = 12, scale = 6)
    private BigDecimal ratePercent;

    @Column(name = "fi_daily_liquidity")
    private Boolean dailyLiquidity;

    @Column(name = "fi_maturity_date")
    private LocalDate maturityDate;
}
