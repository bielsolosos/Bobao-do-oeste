package br.dev.bielsolosos.biscraper.infrastructure.client.email;

import br.dev.bielsolosos.biscraper.infrastructure.BiScraperProperties;
import br.dev.bielsolosos.biscraper.infrastructure.client.email.dto.EmailSendRequest;
import br.dev.bielsolosos.biscraper.infrastructure.client.email.dto.EmailSendResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class CloudflareEmailHttpClient {

    private final RestClient restClient;
    private final BiScraperProperties properties;

    public CloudflareEmailHttpClient(RestClient.Builder restClientBuilder, BiScraperProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    public EmailSendResponse sendEmail(EmailSendRequest request) {
        String baseUrl = properties.getEmail().getWorker().getBaseUrl();
        String authToken = properties.getEmail().getWorker().getAuthToken();
        String endpoint = baseUrl.replaceAll("/+$", "") + "/send-email";

        log.info("[EmailWorkerClient] Enviando requisição HTTP POST para '{}' | Destinatário: '{}' | Assunto: '{}'",
                endpoint, request.to(), request.subject());

        EmailSendResponse response = restClient.post()
                .uri(endpoint)
                .header("x-auth-token", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(EmailSendResponse.class);

        log.info("[EmailWorkerClient] Resposta recebida do Worker para '{}': {}", request.to(), response);
        return response;
    }
}
