package br.dev.bielsolosos.biscraper.api.controller.scraper;

import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.ScraperHttpClient;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto.ScraperQueueStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Scraper Queue", description = "Monitoramento da fila do Scraper Python")
@RestController
@RequestMapping("/api/v1/scraper")
@RequiredArgsConstructor
public class ScraperQueueController {

    private final ScraperHttpClient scraperHttpClient;

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obter status em tempo real da fila de execução do Scraper Python", security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/queue-status")
    public ResponseEntity<ScraperQueueStatusResponse> getQueueStatus() {
        return ResponseEntity.ok(scraperHttpClient.getQueueStatus());
    }
}
