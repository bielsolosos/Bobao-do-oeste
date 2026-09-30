package br.dev.bielsolosos.biscraper.infrastructure.client.email;

import br.dev.bielsolosos.biscraper.infrastructure.BiScraperProperties;
import br.dev.bielsolosos.biscraper.infrastructure.client.email.dto.EmailSendRequest;
import br.dev.bielsolosos.biscraper.infrastructure.client.email.dto.EmailSendResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudflareEmailHttpClientTest {

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

    private BiScraperProperties properties;
    private CloudflareEmailHttpClient client;

    @BeforeEach
    void setUp() {
        properties = new BiScraperProperties();
        properties.getEmail().getWorker().setBaseUrl("http://localhost:8787");
        properties.getEmail().getWorker().setAuthToken("secret-token-123");

        when(restClientBuilder.build()).thenReturn(restClient);
        client = new CloudflareEmailHttpClient(restClientBuilder, properties);
    }

    @Test
    @DisplayName("Deve enviar requisição POST para o Cloudflare Worker com headers corretos")
    void shouldSendEmailViaWorkerSuccessfully() {
        EmailSendRequest request = new EmailSendRequest("user@test.com", "Assunto", "<p>HTML</p>", "Texto");
        EmailSendResponse mockResponse = new EmailSendResponse(true, "msg-123", null);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("http://localhost:8787/send-email")).thenReturn(requestBodySpec);
        when(requestBodySpec.header("x-auth-token", "secret-token-123")).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(requestBodySpec);
        when(requestBodySpec.body(request)).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(EmailSendResponse.class)).thenReturn(mockResponse);

        EmailSendResponse response = client.sendEmail(request);

        assertNotNull(response);
        assertTrue(response.success());
        assertEquals("msg-123", response.messageId());

        verify(restClient, times(1)).post();
        verify(requestBodyUriSpec, times(1)).uri("http://localhost:8787/send-email");
        verify(requestBodySpec, times(1)).header("x-auth-token", "secret-token-123");
    }
}
