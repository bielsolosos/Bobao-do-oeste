package br.dev.bielsolosos.biscraper.api.controller.webhook;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapeResponseDTO;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.webhook.WebhookIncomingPayload;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.WebhookEventRepository;
import br.dev.bielsolosos.biscraper.domain.monitoring.service.WebhookScrapperService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WebhookControllerTest {

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private WebhookScrapperService webhookScrapperService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private WebhookController webhookController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        mockMvc = MockMvcBuilders.standaloneSetup(webhookController)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/webhooks/scraper - Deve receber webhook do scraper e retornar 200 OK com WebhookAckResponse")
    void handleScraperWebhookSuccess() throws Exception {
        String requestId = UUID.randomUUID().toString();
        WebhookIncomingPayload payload = new WebhookIncomingPayload(
                requestId,
                "job-123",
                "SUCCESS",
                new ScrapeResponseDTO(true, null, Collections.emptyList())
        );

        doNothing().when(webhookScrapperService).processScrappingEvent(any(WebhookIncomingPayload.class));

        mockMvc.perform(post("/api/v1/webhooks/scraper")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.requestId").value(requestId))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(webhookScrapperService).processScrappingEvent(any(WebhookIncomingPayload.class));
    }

    @Test
    @DisplayName("GET /api/v1/webhooks/events - Deve retornar página de eventos")
    void listEventsSuccess() throws Exception {
        org.springframework.data.domain.Page<br.dev.bielsolosos.biscraper.domain.monitoring.model.WebhookEvent> page =
                new org.springframework.data.domain.PageImpl<>(Collections.emptyList(), org.springframework.data.domain.PageRequest.of(0, 20), 0);
        org.mockito.Mockito.doReturn(page).when(webhookEventRepository)
                .findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<br.dev.bielsolosos.biscraper.domain.monitoring.model.WebhookEvent>>any(), any(org.springframework.data.domain.Pageable.class));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/webhooks/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
