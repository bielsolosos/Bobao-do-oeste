package br.dev.bielsolosos.biscraper.api.controller.webhook;

import br.dev.bielsolosos.biscraper.core.enums.WebhookStatus;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.webhook.WebhookAckResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.webhook.WebhookEventSummaryResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.webhook.WebhookIncomingPayload;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.WebhookEventRepository;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.WebhookEventSpecifications;
import br.dev.bielsolosos.biscraper.domain.monitoring.service.WebhookScrapperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@Slf4j
@Tag(name = "Webhooks", description = "Endpoints para recebimento assíncrono de notificações e resultados do Scraper")
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookScrapperService scrapperService;

    @Operation(summary = "Recebe o resultado de um job de scraping finalizado pelo Scraper Python")
    @PostMapping("/scraper")
    public ResponseEntity<WebhookAckResponse> handleScraperWebhook(@RequestBody WebhookIncomingPayload payload) {

        scrapperService.processScrappingEvent(payload);

        WebhookAckResponse ack = new WebhookAckResponse(
            "RECEIVED",
            payload.requestId() != null ? payload.requestId() : "",
            OffsetDateTime.now()
        );
        return ResponseEntity.ok(ack);
    }

    @Operation(summary = "Lista o histórico de eventos de webhooks recebidos, com filtros opcionais por status e busca")
    @GetMapping("/events")
    public ResponseEntity<Page<WebhookEventSummaryResponse>> listEvents(
            @RequestParam(value = "status", required = false) WebhookStatus status,
            @RequestParam(value = "q", required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        Page<WebhookEventSummaryResponse> page = webhookEventRepository
            .findAll(WebhookEventSpecifications.filter(status, keyword), pageable)
            .map(event -> new WebhookEventSummaryResponse(
                event.getId(),
                event.getStatus().name(),
                event.getRequestId(),
                event.getJobId(),
                event.getStatus(),
                event.getProcessedAt(),
                event.getCreatedAt()
            ));
        return ResponseEntity.ok(page);
    }
}
