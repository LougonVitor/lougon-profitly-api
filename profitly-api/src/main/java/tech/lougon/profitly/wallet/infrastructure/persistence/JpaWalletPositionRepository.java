package tech.lougon.profitly.wallet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JpaWalletPositionRepository extends JpaRepository<WalletPositionJpaEntity, String> {

    @Query("SELECT DISTINCT p.ticker FROM WalletPositionJpaEntity p")
    List<String> findDistinctTickers();
}
