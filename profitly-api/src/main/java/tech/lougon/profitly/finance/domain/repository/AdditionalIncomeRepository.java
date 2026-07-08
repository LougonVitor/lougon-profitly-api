package tech.lougon.profitly.finance.domain.repository;

import tech.lougon.profitly.finance.domain.model.AdditionalIncome;

import java.util.List;
import java.util.Optional;

public interface AdditionalIncomeRepository {
    AdditionalIncome save(AdditionalIncome additionalIncome);
    List<AdditionalIncome> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<AdditionalIncome> findByIdAndUserId(Long id, String userId);
    void deleteById(Long id);
}
