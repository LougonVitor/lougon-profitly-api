package tech.lougon.profitly.wallet.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Contract terms of a renda-fixa position (CDB/LCI/LCA/LC/LF/RDB). {@code ratePercent}
 * means "% of the indexer" for CDI/SELIC (e.g. 110 = 110% do CDI) or a fixed annual rate
 * for PREFIXADO/IPCA (e.g. 6 = 6% a.a., or the IPCA+ spread).
 */
public record FixedIncomeDetails(
        String issuer,
        FixedIncomeInstrumentType instrumentType,
        Indexer indexer,
        BigDecimal ratePercent,
        boolean dailyLiquidity,
        LocalDate maturityDate
) {
    public String displayName() {
        return instrumentType + (issuer != null && !issuer.isBlank() ? " " + issuer : "");
    }
}
