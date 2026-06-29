package tech.lougon.profitly.wallet.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import tech.lougon.profitly.wallet.domain.model.Wallet;
import tech.lougon.profitly.wallet.domain.model.WalletPosition;
import tech.lougon.profitly.wallet.infrastructure.persistence.WalletJpaEntity;
import tech.lougon.profitly.wallet.infrastructure.persistence.WalletPositionJpaEntity;

import java.util.List;

@Component
public class InfraWalletMapper {

    public Wallet toDomain(WalletJpaEntity entity) {
        List<WalletPosition> positions = entity.getPositions().stream()
                .map(this::toPositionDomain)
                .toList();

        return new Wallet(
                entity.getId(),
                entity.getName(),
                entity.getUserId(),
                positions,
                entity.getCreatedAt()
        );
    }

    public WalletJpaEntity toEntity(Wallet wallet) {
        WalletJpaEntity entity = new WalletJpaEntity();
        entity.setId(wallet.id());
        entity.setName(wallet.name());
        entity.setUserId(wallet.userId());
        entity.setCreatedAt(wallet.createdAt());

        List<WalletPositionJpaEntity> positionEntities = wallet.positions().stream()
                .map(position -> toPositionEntity(position, entity))
                .toList();

        entity.setPositions(positionEntities);
        return entity;
    }

    private WalletPosition toPositionDomain(WalletPositionJpaEntity entity) {
        return new WalletPosition(
                entity.getId(),
                entity.getWallet().getId(),
                entity.getTicker(),
                entity.getQuantity(),
                entity.getAveragePrice(),
                entity.getCreatedAt()
        );
    }

    private WalletPositionJpaEntity toPositionEntity(WalletPosition position, WalletJpaEntity walletEntity) {
        WalletPositionJpaEntity entity = new WalletPositionJpaEntity();
        entity.setId(position.id());
        entity.setWallet(walletEntity);
        entity.setTicker(position.ticker());
        entity.setQuantity(position.quantity());
        entity.setAveragePrice(position.averagePrice());
        entity.setCreatedAt(position.createdAt());
        return entity;
    }
}
