package tech.lougon.profitly.news.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import tech.lougon.profitly.news.application.dto.NewsItemDTO;
import tech.lougon.profitly.news.infrastructure.persistence.JpaNewsRepository;
import tech.lougon.profitly.news.infrastructure.persistence.NewsItemJpaEntity;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    private static final DateTimeFormatter RFC_822 =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern WHITESPACE = Pattern.compile("\\s{2,}");
    private static final Pattern IMG_SRC = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    private static final List<String[]> FEEDS = List.of(
            new String[]{"https://www.infomoney.com.br/feed/", "InfoMoney"},
            new String[]{"https://valoreconomico.globo.com/rss/ultimas-noticias/feed.xml", "Valor Econômico"}
    );

    private final JpaNewsRepository repository;

    public NewsService(JpaNewsRepository repository) {
        this.repository = repository;
    }

    public List<NewsItemDTO> findLatest(int limit) {
        return repository.findTop20ByOrderByPublishedAtDesc()
                .stream().limit(limit).map(NewsItemDTO::from).toList();
    }

    public void sync() {
        log.info("Syncing market news from {} feeds", FEEDS.size());
        int created = 0, updated = 0;
        for (String[] feed : FEEDS) {
            List<NewsItemJpaEntity> items = fetchFeed(feed[0], feed[1]);
            for (NewsItemJpaEntity item : items) {
                Optional<NewsItemJpaEntity> existing = repository.findById(item.getId());
                if (existing.isEmpty()) {
                    repository.save(item);
                    created++;
                } else if (item.getImageUrl() != null && existing.get().getImageUrl() == null) {
                    NewsItemJpaEntity e = existing.get();
                    e.setImageUrl(item.getImageUrl());
                    repository.save(e);
                    updated++;
                }
            }
        }
        log.info("News sync complete — {} new items, {} image updates", created, updated);
    }

    private List<NewsItemJpaEntity> fetchFeed(String feedUrl, String sourceName) {
        List<NewsItemJpaEntity> result = new ArrayList<>();
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(feedUrl).toURL().openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; Profitly/1.0)");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);

            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().parse(conn.getInputStream());
            doc.getDocumentElement().normalize();

            NodeList items = doc.getElementsByTagName("item");
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String title = text(item, "title");
                String link  = text(item, "link");
                if (title == null || link == null) continue;

                String rawDescription = text(item, "description");
                String description = clean(rawDescription);
                String pubDate     = text(item, "pubDate");
                String imageUrl    = imageUrl(item, rawDescription);

                String id = UUID.nameUUIDFromBytes(link.getBytes()).toString();

                var entity = new NewsItemJpaEntity();
                entity.setId(id);
                entity.setTitle(title.trim());
                entity.setDescription(description);
                entity.setUrl(link.trim());
                entity.setSource(sourceName);
                entity.setImageUrl(imageUrl);
                entity.setPublishedAt(parseDate(pubDate));
                entity.setSyncedAt(Instant.now());
                result.add(entity);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch RSS from {} ({}): {}", sourceName, feedUrl, e.getMessage());
        }
        return result;
    }

    private String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent();
    }

    private String imageUrl(Element item, String rawDescription) {
        // Try media:content
        NodeList media = item.getElementsByTagNameNS("*", "content");
        for (int i = 0; i < media.getLength(); i++) {
            Element el = (Element) media.item(i);
            String url    = el.getAttribute("url");
            String medium = el.getAttribute("medium");
            String type   = el.getAttribute("type");
            if (url == null || url.isBlank()) continue;
            boolean isImage = medium.equals("image")
                    || type.startsWith("image")
                    || url.matches("(?i).*\\.(jpg|jpeg|png|webp|gif)(\\?.*)?$");
            if (isImage) return url;
        }
        // Try media:thumbnail
        NodeList thumb = item.getElementsByTagNameNS("*", "thumbnail");
        if (thumb.getLength() > 0) {
            String url = ((Element) thumb.item(0)).getAttribute("url");
            if (url != null && !url.isBlank()) return url;
        }
        // Try enclosure with image type
        NodeList enc = item.getElementsByTagName("enclosure");
        for (int i = 0; i < enc.getLength(); i++) {
            Element el = (Element) enc.item(i);
            String type = el.getAttribute("type");
            String url  = el.getAttribute("url");
            if (url != null && !url.isBlank() && type != null && type.startsWith("image")) return url;
        }
        // Try content:encoded for <img src="...">
        NodeList encoded = item.getElementsByTagNameNS("*", "encoded");
        if (encoded.getLength() > 0) {
            String html = encoded.item(0).getTextContent();
            String img = extractImgSrc(html);
            if (img != null) return img;
        }
        // Try description HTML for <img src="...">
        if (rawDescription != null) {
            String img = extractImgSrc(rawDescription);
            if (img != null) return img;
        }
        return null;
    }

    private String extractImgSrc(String html) {
        Matcher m = IMG_SRC.matcher(html);
        while (m.find()) {
            String src = m.group(1).trim();
            if (!src.isBlank() && !src.startsWith("data:")) return src;
        }
        return null;
    }

    private String clean(String html) {
        if (html == null) return null;
        String stripped = HTML_TAG.matcher(html).replaceAll(" ");
        return WHITESPACE.matcher(stripped).replaceAll(" ").trim();
    }

    private Instant parseDate(String pubDate) {
        if (pubDate == null) return Instant.now();
        try {
            return ZonedDateTime.parse(pubDate.trim(), RFC_822).toInstant();
        } catch (DateTimeParseException e) {
            return Instant.now();
        }
    }
}
