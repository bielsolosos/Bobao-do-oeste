package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ExecutionSummaryDTO;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapedListingDTO;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.webhook.WebhookIncomingPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebhookPayloadDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Deve deserializar com sucesso payload do Python contendo datetime naive (sem timezone offset)")
    void shouldDeserializeNaivePythonDateTime() throws Exception {
        String json = """
                {
                    "requestId": "req-abc-123",
                    "jobId": "job-xyz-456",
                    "status": "SUCCESS",
                    "response": {
                        "success": true,
                        "execution": {
                            "id": "exec-1",
                            "vendor": "OLX",
                            "status": "SUCCESS",
                            "duration_ms": 1500,
                            "total_found": 1,
                            "new_items_count": 1,
                            "used_fallback": false,
                            "started_at": "2026-08-27T18:45:31.807374",
                            "finished_at": "2026-08-27T18:45:33.307374"
                        },
                        "items": [
                            {
                                "vendor": "OLX",
                                "vendor_listing_id": "12345",
                                "title": "Thinkpad T480",
                                "price": 2200.0,
                                "url": "https://olx.com.br/item",
                                "published_at": "2026-08-27T10:00:00",
                                "scraped_at": "2026-08-27T18:45:32.100"
                            }
                        ]
                    }
                }
                """;

        WebhookIncomingPayload payload = objectMapper.readValue(json, WebhookIncomingPayload.class);

        assertNotNull(payload);
        assertEquals("req-abc-123", payload.requestId());
        assertNotNull(payload.response());

        ExecutionSummaryDTO execution = payload.response().execution();
        assertNotNull(execution);
        assertNotNull(execution.startedAt());
        assertEquals(2026, execution.startedAt().getYear());
        assertEquals(8, execution.startedAt().getMonthValue());
        assertEquals(27, execution.startedAt().getDayOfMonth());
        assertEquals(18, execution.startedAt().getHour());
        assertEquals(45, execution.startedAt().getMinute());
        assertEquals(31, execution.startedAt().getSecond());

        ScrapedListingDTO item = payload.response().items().get(0);
        assertNotNull(item);
        assertNotNull(item.publishedAt());
        assertNotNull(item.scrapedAt());
    }
}
