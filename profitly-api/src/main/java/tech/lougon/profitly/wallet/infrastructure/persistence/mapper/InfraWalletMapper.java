package tech.lougon.profitly.wallet.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import tech.lougon.profitly.wallet.domain.model.PositionEntry;
import tech.lougon.profitly.wallet.domain.model.Wallet;
import tech.lougon.profitly.wallet.domain.model.WalletPosition;
import tech.lougon.profitly.wallet.infrastructure.persistence.PositionEntryJpaEntity;
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
        List<PositionEntry> entries = entity.getEntries().stream()
                .map(e -> toEntryDomain(e, entity.getId()))
                .toList();

        return new WalletPosition(
                entity.getId(),
                entity.getWallet().getId(),
                entity.getTicker(),
                entries,
                entity.getCreatedAt()
        );
    }

    private PositionEntry toEntryDomain(PositionEntryJpaEntity entity, String walletPositionId) {
        return new PositionEntry(
                entity.getId(),
                walletPositionId,
                entity.getDate(),
                entity.getQuantityDec() != null
                        ? entity.getQuantityDec()
                        : java.math.BigDecimal.valueOf(entity.getQuantity()),
                entity.getPaidPrice(),
                entity.getEntryType() != null
                        ? tech.lougon.profitly.wallet.domain.model.EntryType.valueOf(entity.getEntryType())
                        : tech.lougon.profitly.wallet.domain.model.EntryType.BUY,
                entity.getCreatedAt()
        );
    }

    private WalletPositionJpaEntity toPositionEntity(WalletPosition position, WalletJpaEntity walletEntity) {
        WalletPositionJpaEntity entity = new WalletPositionJpaEntity();
        entity.setId(position.id());
        entity.setWallet(walletEntity);
        entity.setTicker(position.ticker());
        entity.setCreatedAt(position.createdAt());

        List<PositionEntryJpaEntity> entryEntities = position.entries().stream()
                .map(e -> toEntryEntity(e, entity))
                .toList();

        entity.setEntries(entryEntities);
        return entity;
    }

    private PositionEntryJpaEntity toEntryEntity(PositionEntry entry, WalletPositionJpaEntity positionEntity) {
        PositionEntryJpaEntity entity = new PositionEntryJpaEntity();
        entity.setId(entry.id());
        entity.setWalletPosition(positionEntity);
        entity.setDate(entry.date());
        // legacy integer column stays NOT NULL — store the rounded value alongside the exact one
        entity.setQuantity(entry.quantity().setScale(0, java.math.RoundingMode.HALF_UP).intValue());
        entity.setQuantityDec(entry.quantity());
        entity.setPaidPrice(entry.paidPrice());
        entity.setEntryType(entry.typeOrBuy().name());
        entity.setCreatedAt(entry.createdAt());
        return entity;
    }
}
