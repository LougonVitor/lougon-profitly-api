package tech.lougon.profitly.wallet.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import tech.lougon.profitly.wallet.domain.model.Dividend;
import tech.lougon.profitly.wallet.domain.repository.DividendRepository;

import java.util.List;
import java.util.Optional;

@Repository("walletDividendRepositoryImpl")
public class DividendRepositoryImpl implements DividendRepository {

    private final JpaDividendRepository jpa;

    public DividendRepositoryImpl(JpaDividendRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Dividend save(Dividend d) {
        return toDomain(jpa.save(toEntity(d)));
    }

    @Override
    public List<Dividend> findByWalletId(String walletId) {
        return jpa.findByWalletIdOrderByPaymentDateDesc(walletId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Dividend> findById(String id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(String id) {
        jpa.deleteById(id);
    }

    private DividendJpaEntity toEntity(Dividend d) {
        var e = new DividendJpaEntity();
        e.setId(d.id());
        e.setWalletId(d.walletId());
        e.setUserId(d.userId());
        e.setTicker(d.ticker());
        e.setTotalAmount(d.totalAmount());
        e.setPaymentDate(d.paymentDate());
        e.setType(d.type());
        e.setReceived(d.received());
        e.setCreatedAt(d.createdAt());
        return e;
    }

    private Dividend toDomain(DividendJpaEntity e) {
        return new Dividend(e.getId(), e.getWalletId(), e.getUserId(), e.getTicker(),
                e.getTotalAmount(), e.getPaymentDate(), e.getType(), e.isReceived(), e.getCreatedAt());
    }
}
