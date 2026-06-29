package tech.lougon.profitly.wallet.domain.repository;

import tech.lougon.profitly.wallet.domain.model.Wallet;

import java.util.List;
import java.util.Optional;

public interface WalletRepository {
    Wallet save(Wallet wallet);
    Optional<Wallet> findById(String id);
    List<Wallet> findAll();
}
