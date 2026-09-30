package br.dev.bielsolosos.biscraper.domain.monitoring.service;

import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.core.enums.ExecutionStatus;
import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import br.dev.bielsolosos.biscraper.core.enums.WebhookStatus;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.AnalisyFactorySelector;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.impl.AnalisysFactoryNoneImpl;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapedListing;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapingExecution;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.WebhookEvent;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapedListingDTO;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapeResponseDTO;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.webhook.WebhookIncomingPayload;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ProductMonitorRepository;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ScrapedListingRepository;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ScrapingExecutionRepository;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.WebhookEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookScrapperServiceTest {

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private ScrapingExecutionRepository scrapingExecutionRepository;

    @Mock
    private ScrapedListingRepository scrapedListingRepository;

    @Mock
    private ProductMonitorRepository productMonitorRepository;

    @Spy
    private AnalisyFactorySelector analisysSelector = new AnalisyFactorySelector(List.of(new AnalisysFactoryNoneImpl()));

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private WebhookScrapperService webhookScrapperService;

    private ProductMonitor monitor;
    private ScrapingExecution execution;
    private WebhookEvent webhookEvent;

    @BeforeEach
    void setUp() {
        monitor = ProductMonitor.builder()
                .id(UUID.randomUUID())
                .name("Macbook Pro Monitor")
                .analysisType(AnalysisType.NONE)
                .targetVendor(Vendor.MERCADO_LIVRE)
                .build();

        execution = ScrapingExecution.builder()
                .id(UUID.randomUUID())
                .productMonitor(monitor)
                .vendor(Vendor.MERCADO_LIVRE)
                .status(ExecutionStatus.PENDING)
                .build();

        webhookEvent = WebhookEvent.builder()
                .id(UUID.randomUUID())
                .requestId("req-123")
                .status(WebhookStatus.RECEIVED)
                .build();
    }

    @Test
    @DisplayName("Deve processar evento com novos anúncios e atualizar execução e monitor")
    void processScrappingEventWithNewListings() {
        ScrapedListingDTO item1 = new ScrapedListingDTO(
                "item-1",
                Vendor.MERCADO_LIVRE,
                "MLB12345",
                "Macbook Pro M1 16GB",
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(5500),
                "https://mercadolivre.com.br/item1",
                "Descrição teste",
                "SP",
                "São Paulo",
                "Centro",
                true,
                "MERCADO_ENVIOS",
                List.of("https://img.com/1.jpg"),
                "2026-08-27T18:45:31.807374",
                "2026-08-27T18:45:31.807374"
        );

        ScrapeResponseDTO scrapeResponse = new ScrapeResponseDTO(true, null, List.of(item1));
        WebhookIncomingPayload payload = new WebhookIncomingPayload("req-123", "job-123", "SUCCESS", scrapeResponse);

        when(webhookEventRepository.findByRequestId("req-123")).thenReturn(Optional.of(webhookEvent));
        when(scrapingExecutionRepository.findByWebhookEventRequestId("req-123")).thenReturn(Optional.of(execution));
        when(scrapedListingRepository.findByProductMonitorIdAndVendorAndVendorListingId(monitor.getId(), Vendor.MERCADO_LIVRE, "MLB12345"))
                .thenReturn(Optional.empty());

        webhookScrapperService.processScrappingEvent(payload);

        verify(webhookEventRepository, times(1)).save(webhookEvent);
        assertEquals(WebhookStatus.PROCESSED, webhookEvent.getStatus());

        ArgumentCaptor<List<ScrapedListing>> listingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(scrapedListingRepository, times(1)).saveAll(listingsCaptor.capture());

        List<ScrapedListing> savedListings = listingsCaptor.getValue();
        assertEquals(1, savedListings.size());
        assertEquals("MLB12345", savedListings.get(0).getVendorListingId());
        assertEquals(BigDecimal.valueOf(5000), savedListings.get(0).getCurrentPrice());
        assertEquals(MatchTier.NONE, savedListings.get(0).getMatchTier());

        verify(scrapingExecutionRepository, times(1)).save(execution);
        assertEquals(ExecutionStatus.SUCCESS, execution.getStatus());
        assertEquals(1, execution.getTotalFound());
        assertEquals(1, execution.getNewItemsCount());

        verify(productMonitorRepository, times(1)).save(monitor);
        assertNotNull(monitor.getLastScrapedAt());
    }

    @Test
    @DisplayName("Deve atualizar anúncio existente sem criar duplicidade")
    void processScrappingEventWithExistingListing() {
        ScrapedListing existingListing = ScrapedListing.builder()
                .id(UUID.randomUUID())
                .productMonitor(monitor)
                .vendor(Vendor.MERCADO_LIVRE)
                .vendorListingId("MLB12345")
                .title("Macbook Pro Antigo")
                .currentPrice(BigDecimal.valueOf(6000))
                .url("https://mercadolivre.com.br/item1")
                .build();

        ScrapedListingDTO item1 = new ScrapedListingDTO(
                "item-1",
                Vendor.MERCADO_LIVRE,
                "MLB12345",
                "Macbook Pro M1 16GB",
                BigDecimal.valueOf(4800),
                null,
                "https://mercadolivre.com.br/item1",
                "Descrição atualizada",
                "SP",
                "São Paulo",
                "Centro",
                true,
                "MERCADO_ENVIOS",
                List.of("https://img.com/1.jpg"),
                "2026-08-27T18:45:31.807374",
                "2026-08-27T18:45:31.807374"
        );

        ScrapeResponseDTO scrapeResponse = new ScrapeResponseDTO(true, null, List.of(item1));
        WebhookIncomingPayload payload = new WebhookIncomingPayload("req-123", "job-123", "SUCCESS", scrapeResponse);

        when(webhookEventRepository.findByRequestId("req-123")).thenReturn(Optional.of(webhookEvent));
        when(scrapingExecutionRepository.findByWebhookEventRequestId("req-123")).thenReturn(Optional.of(execution));
        when(scrapedListingRepository.findByProductMonitorIdAndVendorAndVendorListingId(monitor.getId(), Vendor.MERCADO_LIVRE, "MLB12345"))
                .thenReturn(Optional.of(existingListing));

        webhookScrapperService.processScrappingEvent(payload);

        ArgumentCaptor<List<ScrapedListing>> listingsCaptor = ArgumentCaptor.forClass(List.class);
        verify(scrapedListingRepository, times(1)).saveAll(listingsCaptor.capture());

        List<ScrapedListing> savedListings = listingsCaptor.getValue();
        assertEquals(1, savedListings.size());
        assertEquals(BigDecimal.valueOf(4800), savedListings.get(0).getCurrentPrice());
        assertEquals(0, execution.getNewItemsCount());
        assertEquals(1, execution.getTotalFound());
    }

    @Test
    @DisplayName("Deve descartar ingestão de anúncios se o monitor estiver desativado")
    void processScrappingEventWithInactiveMonitor() {
        monitor.setActive(false);

        ScrapedListingDTO item1 = new ScrapedListingDTO(
                "item-1",
                Vendor.MERCADO_LIVRE,
                "MLB12345",
                "Macbook Pro M1",
                BigDecimal.valueOf(5000),
                null,
                "https://mercadolivre.com.br/item1",
                "Descrição",
                "SP",
                "São Paulo",
                "Centro",
                true,
                "MERCADO_ENVIOS",
                List.of("https://img.com/1.jpg"),
                "2026-08-27T18:45:31.807374",
                "2026-08-27T18:45:31.807374"
        );

        ScrapeResponseDTO scrapeResponse = new ScrapeResponseDTO(true, null, List.of(item1));
        WebhookIncomingPayload payload = new WebhookIncomingPayload("req-123", "job-123", "SUCCESS", scrapeResponse);

        when(webhookEventRepository.findByRequestId("req-123")).thenReturn(Optional.of(webhookEvent));
        when(scrapingExecutionRepository.findByWebhookEventRequestId("req-123")).thenReturn(Optional.of(execution));

        webhookScrapperService.processScrappingEvent(payload);

        verifyNoInteractions(scrapedListingRepository);
        verify(productMonitorRepository, never()).save(any(ProductMonitor.class));
        verify(scrapingExecutionRepository, times(1)).save(execution);
        assertEquals(ExecutionStatus.SUCCESS, execution.getStatus());
        assertEquals(0, execution.getNewItemsCount());
    }
}

