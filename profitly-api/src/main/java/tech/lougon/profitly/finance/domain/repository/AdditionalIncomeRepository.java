package tech.lougon.profitly.finance.domain.repository;

import tech.lougon.profitly.finance.domain.model.AdditionalIncome;

import java.util.List;

public interface AdditionalIncomeRepository {
    AdditionalIncome save(AdditionalIncome additionalIncome);
    List<AdditionalIncome> findByUserIdOrderByCreatedAtDesc(String userId);
    void deleteById(Long id);
}
