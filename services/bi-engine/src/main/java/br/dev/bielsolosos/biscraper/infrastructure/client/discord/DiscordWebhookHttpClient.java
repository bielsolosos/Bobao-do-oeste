package br.dev.bielsolosos.biscraper.infrastructure.client.discord;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class DiscordWebhookHttpClient {

    private final RestClient restClient;

    public DiscordWebhookHttpClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public void sendWebhook(String webhookUrl, Map<String, Object> payload) {
        log.debug("Enviando webhook Discord para URL configurada.");
        restClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
