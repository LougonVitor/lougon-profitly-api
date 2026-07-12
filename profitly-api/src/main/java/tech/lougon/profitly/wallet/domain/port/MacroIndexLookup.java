package tech.lougon.profitly.wallet.domain.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Reads locally-stored CDI/Selic/IPCA daily-index observations — never calls brapi directly. */
public interface MacroIndexLookup {

    /** Observations for {@code slug} with {@code start <= date <= end}, ordered ascending by date. */
    List<IndexPoint> findObservations(String slug, LocalDate start, LocalDate end);

    record IndexPoint(LocalDate date, BigDecimal value) {}
}
