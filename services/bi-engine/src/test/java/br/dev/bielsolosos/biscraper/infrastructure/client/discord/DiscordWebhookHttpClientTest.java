package br.dev.bielsolosos.biscraper.infrastructure.client.discord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscordWebhookHttpClientTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private DiscordWebhookHttpClient discordWebhookHttpClient;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.build()).thenReturn(restClient);
        discordWebhookHttpClient = new DiscordWebhookHttpClient(restClientBuilder);
    }

    @Test
    @DisplayName("Deve disparar POST HTTP com o payload e URL corretos")
    void shouldSendWebhookCorrectly() {
        String webhookUrl = "https://discord.com/api/webhooks/123/token";
        Map<String, Object> payload = Map.of("content", "Test message");

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(webhookUrl)).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(payload)).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        discordWebhookHttpClient.sendWebhook(webhookUrl, payload);

        verify(restClient, times(1)).post();
        verify(requestBodyUriSpec, times(1)).uri(webhookUrl);
        verify(requestBodySpec, times(1)).contentType(MediaType.APPLICATION_JSON);
        verify(requestBodySpec, times(1)).body(payload);
        verify(responseSpec, times(1)).toBodilessEntity();
    }
}
