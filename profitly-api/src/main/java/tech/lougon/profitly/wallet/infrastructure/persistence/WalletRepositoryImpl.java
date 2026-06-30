package tech.lougon.profitly.wallet.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import tech.lougon.profitly.wallet.domain.model.Wallet;
import tech.lougon.profitly.wallet.domain.repository.WalletRepository;
import tech.lougon.profitly.wallet.infrastructure.persistence.mapper.InfraWalletMapper;

import java.util.List;
import java.util.Optional;

@Repository
public class WalletRepositoryImpl implements WalletRepository {

    private final JpaWalletRepository jpaWalletRepository;
    private final InfraWalletMapper walletMapper;

    public WalletRepositoryImpl(JpaWalletRepository jpaWalletRepository, InfraWalletMapper walletMapper) {
        this.jpaWalletRepository = jpaWalletRepository;
        this.walletMapper = walletMapper;
    }

    @Override
    public Wallet save(Wallet wallet) {
        WalletJpaEntity entity = walletMapper.toEntity(wallet);
        return walletMapper.toDomain(jpaWalletRepository.save(entity));
    }

    @Override
    public Optional<Wallet> findById(String id) {
        return jpaWalletRepository.findById(id).map(walletMapper::toDomain);
    }

    @Override
    public List<Wallet> findByUserId(String userId) {
        return jpaWalletRepository.findByUserId(userId).stream()
                .map(walletMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(String id) {
        jpaWalletRepository.deleteById(id);
    }
}
