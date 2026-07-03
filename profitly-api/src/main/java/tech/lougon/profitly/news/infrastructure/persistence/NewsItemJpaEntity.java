package tech.lougon.profitly.news.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "news_items")
public class NewsItemJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String url;

    private String source;
    private String imageUrl;
    private Instant publishedAt;
    private Instant syncedAt;

    public NewsItemJpaEntity() {}

    public String getId() { return id; }
    public void setId(String v) { this.id = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getUrl() { return url; }
    public void setUrl(String v) { this.url = v; }
    public String getSource() { return source; }
    public void setSource(String v) { this.source = v; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String v) { this.imageUrl = v; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant v) { this.publishedAt = v; }
    public Instant getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Instant v) { this.syncedAt = v; }
}
