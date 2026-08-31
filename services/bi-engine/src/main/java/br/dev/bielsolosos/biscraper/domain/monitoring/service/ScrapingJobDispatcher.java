package br.dev.bielsolosos.biscraper.domain.monitoring.service;

import br.dev.bielsolosos.biscraper.core.enums.ExecutionStatus;
import br.dev.bielsolosos.biscraper.core.enums.WebhookStatus;
import br.dev.bielsolosos.biscraper.domain.monitoring.event.MonitorCreatedEvent;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.MonitorSearchQuery;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapingExecution;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ScrapingExecutionRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapingJobDispatcher {

    private final ScraperHttpClient scraperHttpClient;
    private final WebhookEventRepository webhookEventRepository;
    private final ScrapingExecutionRepository scrapingExecutionRepository;
    private final BiScraperProperties properties;
    private final ObjectMapper objectMapper;
    private final org.springframework.transaction.support.TransactionTemplate requiresNewTransactionTemplate;

    public ScrapingJobDispatcher(ScraperHttpClient scraperHttpClient, 
                                 WebhookEventRepository webhookEventRepository, 
                                 ScrapingExecutionRepository scrapingExecutionRepository, 
                                 BiScraperProperties properties, 
                                 ObjectMapper objectMapper, 
                                 org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.scraperHttpClient = scraperHttpClient;
        this.webhookEventRepository = webhookEventRepository;
        this.scrapingExecutionRepository = scrapingExecutionRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.requiresNewTransactionTemplate = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        this.requiresNewTransactionTemplate.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

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
        String requestId = UUID.randomUUID().toString();
        try {
            if (monitor.getId() != null) MDC.put("monitorId", monitor.getId().toString());
            MDC.put("keyword", query.getQueryTerm());
            MDC.put("requestId", requestId);

            log.info("Criando ScrapingExecution e WebhookEvent com requestId '{}'", requestId);

            WebhookEvent webhookEvent = requiresNewTransactionTemplate.execute(status -> {
                // 1. Registra o evento de Webhook inicial para rastreamento
                WebhookEvent we = WebhookEvent.builder()
                        .requestId(requestId)
                        .source("SCRAPER_PYTHON")
                        .status(WebhookStatus.RECEIVED)
                        .rawPayload(objectMapper.createObjectNode())
                        .build();
                we = webhookEventRepository.save(we);

                // 2. Registra a Execução pendente amarrada ao Monitor, Query e Webhook
                ScrapingExecution execution = ScrapingExecution.builder()
                        .productMonitor(monitor)
                        .searchQuery(query)
                        .webhookEvent(we)
                        .vendor(monitor.getTargetVendor())
                        .status(ExecutionStatus.PENDING)
                        .build();
                scrapingExecutionRepository.save(execution);
                
                return we;
            });

            // 3. Monta o payload de envio para o Scraper Python
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
                    properties.getScraper().getWebhookUrl(),
                    requestId
            );

            log.info("Disparando POST /api/v1/scrape/async para o termo '{}' no vendor '{}'...",
                    query.getQueryTerm(), monitor.getTargetVendor());

            AsyncScrapeClientResponse response = scraperHttpClient.dispatchAsyncScrape(request);

            if (response != null && response.jobId() != null) {
                MDC.put("jobId", response.jobId());
                webhookEvent.setJobId(response.jobId());
                webhookEventRepository.save(webhookEvent);
            }

            log.info("Scraper Python enfileirou com sucesso! JobId: '{}'", response != null ? response.jobId() : "N/A");

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
