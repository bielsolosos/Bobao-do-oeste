package br.dev.bielsolosos.biscraper.api.controller.webhook;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.WebhookAckResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.WebhookEventSummaryResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.WebhookIncomingPayload;
import br.dev.bielsolosos.biscraper.core.enums.WebhookStatus;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.WebhookEvent;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.WebhookEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Tag(name = "Webhooks", description = "Endpoints para recebimento assíncrono de notificações e resultados do Scraper")
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Recebe o resultado de um job de scraping finalizado pelo Scraper Python")
    @PostMapping("/scraper")
    public ResponseEntity<WebhookAckResponse> handleScraperWebhook(@RequestBody WebhookIncomingPayload payload) {
        try {
            if (payload.requestId() != null) MDC.put("requestId", payload.requestId());
            if (payload.jobId() != null) MDC.put("jobId", payload.jobId());

            log.info("=================================================");
            log.info(">>> WEBHOOK RECEBIDO DO SCRAPER PYTHON <<<");
            log.info("Status: '{}'", payload.status());

            if (payload.response() != null) {
                Object items = payload.response().get("items");
                int totalItems = (items instanceof List<?> list) ? list.size() : 0;
                log.info("Total de anúncios raspados recebidos no payload: {}", totalItems);
            }
            log.info("=================================================");

            // Atualiza ou registra o evento recebido no banco
            Optional<WebhookEvent> existingEvent = webhookEventRepository.findByRequestId(payload.requestId());
            if (existingEvent.isPresent()) {
                WebhookEvent event = existingEvent.get();
                event.setStatus("SUCCESS".equalsIgnoreCase(payload.status()) ? WebhookStatus.PROCESSED : WebhookStatus.FAILED);
                if (payload.response() != null) {
                    event.setRawPayload(objectMapper.valueToTree(payload.response()));
                }
                event.setProcessedAt(OffsetDateTime.now());
                webhookEventRepository.save(event);
                log.debug("WebhookEvent atualizado com sucesso no banco");
            }

            WebhookAckResponse ack = new WebhookAckResponse(
                    "RECEIVED",
                    payload.requestId() != null ? payload.requestId() : "",
                    OffsetDateTime.now()
            );

            return ResponseEntity.ok(ack);
        } finally {
            MDC.remove("requestId");
            MDC.remove("jobId");
        }
    }

    @Operation(summary = "Lista o histórico de eventos de webhooks recebidos")
    @GetMapping("/events")
    public ResponseEntity<Page<WebhookEventSummaryResponse>> listEvents(
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        
        Page<WebhookEventSummaryResponse> page = webhookEventRepository.findAll(pageable)
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
