package tech.lougon.profitly.news.application.dto;

import tech.lougon.profitly.news.infrastructure.persistence.NewsItemJpaEntity;

import java.time.Instant;

public record NewsItemDTO(
        String id,
        String title,
        String description,
        String url,
        String source,
        String imageUrl,
        Instant publishedAt
) {
    public static NewsItemDTO from(NewsItemJpaEntity e) {
        return new NewsItemDTO(e.getId(), e.getTitle(), e.getDescription(),
                e.getUrl(), e.getSource(), e.getImageUrl(), e.getPublishedAt());
    }
}
