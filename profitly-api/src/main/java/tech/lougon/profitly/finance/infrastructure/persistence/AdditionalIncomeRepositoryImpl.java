package tech.lougon.profitly.finance.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import tech.lougon.profitly.finance.domain.model.AdditionalIncome;
import tech.lougon.profitly.finance.domain.repository.AdditionalIncomeRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class AdditionalIncomeRepositoryImpl implements AdditionalIncomeRepository {

    private final JpaAdditionalIncomeRepository jpa;

    public AdditionalIncomeRepositoryImpl(JpaAdditionalIncomeRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AdditionalIncome save(AdditionalIncome additionalIncome) {
        return toDomain(jpa.save(toEntity(additionalIncome)));
    }

    @Override
    public List<AdditionalIncome> findByUserIdOrderByCreatedAtDesc(String userId) {
        return jpa.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<AdditionalIncome> findByIdAndUserId(Long id, String userId) {
        return jpa.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    private AdditionalIncomeJpaEntity toEntity(AdditionalIncome a) {
        var e = new AdditionalIncomeJpaEntity();
        e.setId(a.id());
        e.setUserId(a.userId());
        e.setDescription(a.description());
        e.setAmount(a.amount());
        e.setCreatedAt(a.createdAt());
        return e;
    }

    private AdditionalIncome toDomain(AdditionalIncomeJpaEntity e) {
        return new AdditionalIncome(e.getId(), e.getUserId(), e.getDescription(), e.getAmount(), e.getCreatedAt());
    }
}
