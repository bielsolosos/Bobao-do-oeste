package br.dev.bielsolosos.biscraper.infrastructure.client.scraper;

import br.dev.bielsolosos.biscraper.infrastructure.BiScraperProperties;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto.AsyncScrapeClientRequest;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto.AsyncScrapeClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
public class ScraperHttpClient {

    private final RestClient restClient;
    private final BiScraperProperties properties;

    public ScraperHttpClient(BiScraperProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getScraper().getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public AsyncScrapeClientResponse dispatchAsyncScrape(AsyncScrapeClientRequest request) {
        String authHeader = getBasicAuthHeader();

        log.debug("Enviando POST /api/v1/scrape/async para o Scraper em: {} com keyword: '{}'",
                properties.getScraper().getBaseUrl(), request.request().keyword());

        return restClient.post()
                .uri("/api/v1/scrape/async")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .body(request)
                .retrieve()
                .body(AsyncScrapeClientResponse.class);
    }

    private String getBasicAuthHeader() {
        String auth = properties.getScraper().getUsername() + ":" + properties.getScraper().getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }
}
