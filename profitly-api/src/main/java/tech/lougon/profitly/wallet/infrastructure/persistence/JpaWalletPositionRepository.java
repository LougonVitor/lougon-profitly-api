package tech.lougon.profitly.wallet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JpaWalletPositionRepository extends JpaRepository<WalletPositionJpaEntity, String> {

    // Excludes renda-fixa positions (synthetic rf-<uuid> tickers) — they have no market
    // data to sync and would otherwise burn brapi calls in the daily price/dividend sync
    // forever, since they never appear in any ticker catalog.
    @Query("SELECT DISTINCT p.ticker FROM WalletPositionJpaEntity p WHERE p.fixedIncomeDetails.indexer IS NULL")
    List<String> findDistinctTickers();
}
