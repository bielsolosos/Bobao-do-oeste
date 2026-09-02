package br.dev.bielsolosos.biscraper.infrastructure.client.scraper;

import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import br.dev.bielsolosos.biscraper.infrastructure.BiScraperProperties;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto.ScrapeDetailRequest;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto.ScrapeDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ScraperHttpClientTest {

    private ScraperHttpClient scraperHttpClient;
    private MockRestServiceServer mockServer;
    private BiScraperProperties properties;

    @BeforeEach
    void setUp() {
        properties = new BiScraperProperties();
        properties.getScraper().setBaseUrl("http://localhost:8000");
        properties.getScraper().setUsername("testuser");
        properties.getScraper().setPassword("testpass");

        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(properties.getScraper().getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();

        scraperHttpClient = new ScraperHttpClient(properties);
        ReflectionTestUtils.setField(scraperHttpClient, "restClient", restClient);
    }

    @Test
    @DisplayName("Deve enviar POST para /api/v1/scrape/detail com Basic Auth e retornar ScrapeDetailResponse")
    void shouldScrapeDetailSuccessfully() {
        String expectedAuth = "Basic " + Base64.getEncoder().encodeToString("testuser:testpass".getBytes(StandardCharsets.UTF_8));
        String responseBody = """
                {
                    "success": true,
                    "from_cache": false,
                    "used_fallback": false,
                    "data": {
                        "vendor": "OLX",
                        "vendor_listing_id": "9999",
                        "url": "https://olx.com.br/item-9999",
                        "title": "Notebook Dell",
                        "price": 3500.0,
                        "description": "Excelente estado",
                        "properties": {
                            "RAM": "16GB"
                        },
                        "images": []
                    },
                    "error_message": null
                }
                """;

        mockServer.expect(requestTo("http://localhost:8000/api/v1/scrape/detail"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, expectedAuth))
                .andExpect(jsonPath("$.url").value("https://olx.com.br/item-9999"))
                .andExpect(jsonPath("$.vendor").value("OLX"))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        ScrapeDetailResponse response = scraperHttpClient.scrapeDetail(new ScrapeDetailRequest("https://olx.com.br/item-9999", Vendor.OLX, false, 24, false));

        assertNotNull(response);
        assertTrue(response.success());
        assertNotNull(response.data());
        assertEquals("Notebook Dell", response.data().title());
        assertEquals("16GB", response.data().properties().get("RAM"));

        mockServer.verify();
    }
}
