package tech.lougon.profitly.wallet.domain.repository;

import tech.lougon.profitly.wallet.domain.model.Dividend;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DividendRepository {
    Dividend save(Dividend dividend);
    List<Dividend> findByWalletId(String walletId);
    Optional<Dividend> findById(String id);
    void deleteById(String id);
    boolean existsByWalletIdAndTickerAndPaymentDate(String walletId, String ticker, LocalDate paymentDate);
}
