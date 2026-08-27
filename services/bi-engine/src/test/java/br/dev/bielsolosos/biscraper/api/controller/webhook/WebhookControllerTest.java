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
        mockMvc = MockMvcBuilders.standaloneSetup(webhookController).build();
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
}
