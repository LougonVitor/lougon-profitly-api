package tech.lougon.profitly.analysis.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.analysis.application.dto.RankingsDTO;
import tech.lougon.profitly.analysis.application.service.RankingsService;

@RestController
@RequestMapping("/api/rankings")
public class RankingsController {

    private final RankingsService service;

    public RankingsController(RankingsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<RankingsDTO> getRankings(
            @RequestParam(required = false) String assetType) {
        return ResponseEntity.ok(service.getRankings(assetType));
    }
}
