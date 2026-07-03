package tech.lougon.profitly.news.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaNewsRepository extends JpaRepository<NewsItemJpaEntity, String> {
    List<NewsItemJpaEntity> findTop20ByOrderByPublishedAtDesc();
}
