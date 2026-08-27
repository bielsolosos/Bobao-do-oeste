package br.dev.bielsolosos.biscraper.domain.monitoring.service;

import br.dev.bielsolosos.biscraper.core.enums.WebhookStatus;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapingExecution;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.WebhookEvent;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.WebhookIncomingPayload;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.WebhookIncomingPayload.ScrapedListingItem;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ScrapingExecutionRepository;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.WebhookEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookScrapperService {

    private final WebhookEventRepository webhookEventRepository;
    private final ScrapingExecutionRepository scrapingExecutionRepository;
    private final ObjectMapper objectMapper;

    @Async("scraperDispatcherExecutor")
    @Transactional
    public void processScrappingEvent(WebhookIncomingPayload payload) {
        try {
            if (payload.requestId() != null) MDC.put("requestId", payload.requestId());
            if (payload.jobId() != null) MDC.put("jobId", payload.jobId());

            log.info("=================================================");
            log.info(">>> WEBHOOK RECEBIDO DO SCRAPER PYTHON <<<");
            log.info("Status: '{}'", payload.status());

            List<ScrapedListingItem> items = (payload.response() != null && payload.response().items() != null)
                    ? payload.response().items()
                    : Collections.emptyList();

            log.info("Total de anúncios raspados recebidos no payload: {}", items.size());
            log.info("=================================================");

            // 1. Atualiza o registro do WebhookEvent no banco
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

            // 2. Localiza a ScrapingExecution vinculada ao requestId
            Optional<ScrapingExecution> executionOpt = scrapingExecutionRepository.findByWebhookEventRequestId(payload.requestId());
            if (executionOpt.isEmpty()) {
                log.warn("Nenhuma ScrapingExecution encontrada para o requestId '{}'.", payload.requestId());
                //TODO pensar se estoura uma exception.
                return;
            }

            ScrapingExecution execution = executionOpt.get();

            log.info("Iniciando análise do payload para Monitor '{}' (ID: {}). Itens: {}", execution.getProductMonitor().getName(), execution.getProductMonitor().getId(), items.size());

            // 3. Invoca a análise do payload passando monitor, execução e anúncios
            analyzePayload(execution, items);

        } catch (Exception e) {
            log.error("Erro inesperado ao processar evento de scraping: {}", e.getMessage(), e);
        } finally {
            MDC.remove("requestId");
            MDC.remove("jobId");
        }
    }

    /**
     * Realiza a análise do payload e ingestão dos anúncios para o monitor
     * correspondente.
     */
    public void analyzePayload(ScrapingExecution execution, List<ScrapedListingItem> listings) {
      ProductMonitor monitor = execution.getProductMonitor();

      log.info("Executando analyzePayload para Monitor '{}' com {} anúncios.", monitor != null ? monitor.getName() : "N/A", listings != null ? listings.size() : 0);
      
    }
}
