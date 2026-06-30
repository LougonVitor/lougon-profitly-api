package tech.lougon.profitly.wallet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaWalletRepository extends JpaRepository<WalletJpaEntity, String> {
    List<WalletJpaEntity> findByUserId(String userId);
}
