package br.dev.bielsolosos.biscraper.api.controller.scraper;

import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.ScraperHttpClient;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto.ScraperQueueStatusResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScraperQueueControllerTest {

    @Mock
    private ScraperHttpClient scraperHttpClient;

    @InjectMocks
    private ScraperQueueController controller;

    @Test
    @DisplayName("Deve retornar status da fila retornado pelo cliente HTTP do Scraper")
    void shouldReturnQueueStatus() {
        ScraperQueueStatusResponse mockResponse = new ScraperQueueStatusResponse(2, 1, 3, 0, 10, 1);
        when(scraperHttpClient.getQueueStatus()).thenReturn(mockResponse);

        ResponseEntity<ScraperQueueStatusResponse> response = controller.getQueueStatus();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockResponse, response.getBody());
    }
}
