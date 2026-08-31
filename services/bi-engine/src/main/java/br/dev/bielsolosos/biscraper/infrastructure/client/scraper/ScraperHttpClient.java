package br.dev.bielsolosos.biscraper.infrastructure.client.scraper;

import br.dev.bielsolosos.biscraper.infrastructure.BiScraperProperties;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto.AsyncScrapeClientRequest;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto.AsyncScrapeClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Slf4j
@Component
public class ScraperHttpClient {

    private final RestClient restClient;
    private final BiScraperProperties properties;

    public ScraperHttpClient(BiScraperProperties properties) {
        this.properties = properties;

        // Força explicitamente o protocolo HTTP/1.1 para evitar headers de upgrade h2c rejeitados pelo Uvicorn/FastAPI
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        this.restClient = RestClient.builder()
                .baseUrl(properties.getScraper().getBaseUrl())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
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

    public br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto.ScraperQueueStatusResponse getQueueStatus() {
        String authHeader = getBasicAuthHeader();
        try {
            return restClient.get()
                    .uri("/api/v1/queue/status")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto.ScraperQueueStatusResponse.class);
        } catch (Exception e) {
            log.warn("Não foi possível obter status da fila do Scraper Python: {}", e.getMessage());
            return new br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto.ScraperQueueStatusResponse(0, 0, 0, 0, 0, 0);
        }
    }

    private String getBasicAuthHeader() {
        String auth = properties.getScraper().getUsername() + ":" + properties.getScraper().getPassword();
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }
}

