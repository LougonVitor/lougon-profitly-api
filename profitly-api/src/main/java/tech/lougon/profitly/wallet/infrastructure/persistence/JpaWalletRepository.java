package tech.lougon.profitly.wallet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaWalletRepository extends JpaRepository<WalletJpaEntity, String> {
}
