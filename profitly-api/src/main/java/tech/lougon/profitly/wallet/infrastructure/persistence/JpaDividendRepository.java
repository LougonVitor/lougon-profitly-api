package tech.lougon.profitly.wallet.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface JpaDividendRepository extends JpaRepository<DividendJpaEntity, String> {
    List<DividendJpaEntity> findByWalletIdOrderByPaymentDateDesc(String walletId);
    void deleteByWalletId(String walletId);
    boolean existsByWalletIdAndTickerAndPaymentDate(String walletId, String ticker, LocalDate paymentDate);
}
