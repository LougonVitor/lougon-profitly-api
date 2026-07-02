package tech.lougon.profitly.analysis.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.lougon.profitly.analysis.application.dto.IbovespaResponse;
import tech.lougon.profitly.analysis.application.service.IbovespaService;

@RestController
@RequestMapping("/api/ibovespa")
public class IbovespaController {

    private final IbovespaService service;

    public IbovespaController(IbovespaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<IbovespaResponse> get(
            @RequestParam(defaultValue = "1d") String range) {
        return ResponseEntity.ok(service.fetch(range));
    }
}
