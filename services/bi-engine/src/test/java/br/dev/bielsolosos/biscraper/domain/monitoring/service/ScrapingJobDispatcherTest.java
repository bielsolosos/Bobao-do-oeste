package br.dev.bielsolosos.biscraper.domain.monitoring.service;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScrapingJobDispatcherTest {

    @Mock
    private ScraperHttpClient scraperHttpClient;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private ScrapingExecutionRepository scrapingExecutionRepository;

    @Spy
    private BiScraperProperties properties = new BiScraperProperties();

    @Mock
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ScrapingJobDispatcher dispatcher;

    private ProductMonitor monitor;
    private MonitorSearchQuery query;

    @BeforeEach
    void setUp() {
        monitor = ProductMonitor.builder()
                .id(UUID.randomUUID())
                .name("Busca Thinkpad")
                .build();

        query = MonitorSearchQuery.builder()
                .id(UUID.randomUUID())
                .queryTerm("thinkpad t480")
                .minPrice(BigDecimal.valueOf(1000))
                .maxPrice(BigDecimal.valueOf(2500))
                .requireDelivery(true)
                .maxPages(1)
                .build();

        monitor.addSearchQuery(query);
    }

    @Test
    @DisplayName("Deve despachar job de scraping com sucesso e salvar WebhookEvent e ScrapingExecution")
    void onMonitorCreatedSuccess() {
        AsyncScrapeClientResponse clientResponse = new AsyncScrapeClientResponse(
                "req-uuid-123",
                "job-999",
                "queued",
                "http://localhost:8080/api/v1/webhooks/scraper"
        );

        when(scraperHttpClient.dispatchAsyncScrape(any(AsyncScrapeClientRequest.class))).thenReturn(clientResponse);
        when(transactionManager.getTransaction(any())).thenReturn(new org.springframework.transaction.support.SimpleTransactionStatus());
        when(webhookEventRepository.save(any(WebhookEvent.class))).thenAnswer(i -> i.getArgument(0));
        when(scrapingExecutionRepository.save(any(ScrapingExecution.class))).thenAnswer(i -> i.getArgument(0));

        dispatcher.onMonitorCreated(new MonitorCreatedEvent(monitor));

        verify(webhookEventRepository, atLeastOnce()).save(any(WebhookEvent.class));
        verify(scrapingExecutionRepository, times(1)).save(any(ScrapingExecution.class));
        verify(scraperHttpClient, times(1)).dispatchAsyncScrape(any(AsyncScrapeClientRequest.class));
    }
}
