package tech.lougon.profitly.news.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.news.application.dto.NewsItemDTO;
import tech.lougon.profitly.news.application.service.NewsService;

import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public ResponseEntity<List<NewsItemDTO>> getNews(
            @RequestParam(defaultValue = "9") int limit) {
        return ResponseEntity.ok(newsService.findLatest(limit));
    }
}
