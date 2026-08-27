package br.dev.bielsolosos.biscraper.domain.monitoring.service;

import br.dev.bielsolosos.biscraper.core.enums.WebhookStatus;
import br.dev.bielsolosos.biscraper.domain.monitoring.event.MonitorCreatedEvent;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.MonitorSearchQuery;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.WebhookEvent;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.WebhookEventRepository;
import br.dev.bielsolosos.biscraper.infrastructure.BiScraperProperties;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.ScraperHttpClient;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto.AsyncScrapeClientRequest;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto.AsyncScrapeClientResponse;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto.ScrapeJobRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapingJobDispatcher {

    private final ScraperHttpClient scraperHttpClient;
    private final WebhookEventRepository webhookEventRepository;
    private final BiScraperProperties properties;
    private final ObjectMapper objectMapper;

    @Async("scraperDispatcherExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMonitorCreated(MonitorCreatedEvent event) {
        ProductMonitor monitor = event.monitor();
        log.info("Processando disparo assíncrono concorrente para monitor '{}' (ID: {}) com {} queries de busca.",
                monitor.getName(), monitor.getId(), monitor.getSearchQueries().size());

        for (MonitorSearchQuery query : monitor.getSearchQueries()) {
            dispatchQuery(monitor, query);
        }
    }

    public void dispatchQuery(ProductMonitor monitor, MonitorSearchQuery query) {
        try {
            if (monitor.getId() != null) MDC.put("monitorId", monitor.getId().toString());
            MDC.put("keyword", query.getQueryTerm());

            ScrapeJobRequest jobRequest = new ScrapeJobRequest(
                    monitor.getTargetVendor().name(),
                    query.getQueryTerm(),
                    query.getStateFilter(),
                    query.getRegionFilter(),
                    query.getCategorySlug(),
                    query.getMinPrice(),
                    query.getMaxPrice(),
                    query.isRequireDelivery(),
                    query.getMaxPages(),
                    false
            );

            AsyncScrapeClientRequest request = new AsyncScrapeClientRequest(
                    jobRequest,
                    properties.getScraper().getWebhookUrl()
            );

            log.info("Disparando scraping assíncrono para o termo '{}' no vendor '{}'...",
                    query.getQueryTerm(), monitor.getTargetVendor());

            AsyncScrapeClientResponse response = scraperHttpClient.dispatchAsyncScrape(request);

            if (response.requestId() != null) MDC.put("requestId", response.requestId());
            if (response.jobId() != null) MDC.put("jobId", response.jobId());

            log.info("Scraper Python enfileirou com sucesso!");

            // Registra o evento de Webhook inicial para auditoria e rastreamento
            WebhookEvent webhookEvent = WebhookEvent.builder()
                    .requestId(response.requestId())
                    .jobId(response.jobId())
                    .source("SCRAPER_PYTHON")
                    .status(WebhookStatus.RECEIVED)
                    .rawPayload(objectMapper.valueToTree(response))
                    .build();

            webhookEventRepository.save(webhookEvent);

        } catch (Exception e) {
            log.error("Erro ao despachar job de scraping para o termo '{}' do monitor '{}': {}",
                    query.getQueryTerm(), monitor.getName(), e.getMessage(), e);
        } finally {
            MDC.remove("monitorId");
            MDC.remove("keyword");
            MDC.remove("requestId");
            MDC.remove("jobId");
        }
    }
}
