package br.dev.bielsolosos.biscraper.domain.monitoring.analysis.impl;

import br.dev.bielsolosos.biscraper.core.enums.*;
import br.dev.bielsolosos.biscraper.domain.ai.service.AiAnalysisLogService;
import br.dev.bielsolosos.biscraper.domain.ai.tools.ScrappingDetailsTools;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.AnalisysResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapingExecution;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapedListingDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalisysFactoryNotebookImplTest {

    @Mock
    private ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient.Builder chatClientBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock
    private AiAnalysisLogService aiAnalysisLogService;

    @Mock
    private ScrappingDetailsTools detailsTools;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ScrapingExecution execution;
    private ProductMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new ProductMonitor();
        monitor.setId(UUID.randomUUID());
        monitor.setName("Dell i7 16GB");

        execution = ScrapingExecution.builder()
                .id(UUID.randomUUID())
                .productMonitor(monitor)
                .build();
    }

    @Test
    @DisplayName("Deve retornar AnalysisType.NOTEBOOK")
    void shouldReturnCorrectAnalysisType() {
        when(chatClientBuilderProvider.getIfAvailable()).thenReturn(null);
        AnalisysFactoryNotebookImpl factory = new AnalisysFactoryNotebookImpl(chatClientBuilderProvider, objectMapper, aiAnalysisLogService, detailsTools);
        assertEquals(AnalysisType.NOTEBOOK, factory.getAnalisysType());
    }

    @Test
    @DisplayName("Deve invocar Gemini e converter resposta para DTOs de notebook")
    void shouldAnalyzeNotebookListingsSuccessfully() {
        when(chatClientBuilderProvider.getIfAvailable()).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        AnalisysFactoryNotebookImpl factory = new AnalisysFactoryNotebookImpl(chatClientBuilderProvider, objectMapper, aiAnalysisLogService, detailsTools);

        ScrapedListingDTO item1 = createListing("item-1", "Dell G15 i7 11800H 16GB SSD 512 RTX 3050 Full HD", BigDecimal.valueOf(3800));

        String jsonAiResponse = """
                [
                    {
                        "brand": "DELL",
                        "processorBrand": "INTEL",
                        "processGeneration": 11,
                        "ramSize": 16,
                        "ramType": "DDR4",
                        "storageSizeGb": 512,
                        "diskType": "SSD_NVME",
                        "screenResolution": "FULL_HD",
                        "hasGpu": true
                    }
                ]
                """;

        org.springframework.ai.chat.model.ChatResponse mockResponse = mock(org.springframework.ai.chat.model.ChatResponse.class);
        org.springframework.ai.chat.model.Generation generation = mock(org.springframework.ai.chat.model.Generation.class);
        org.springframework.ai.chat.messages.AssistantMessage message = mock(org.springframework.ai.chat.messages.AssistantMessage.class);

        when(mockResponse.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(message);
        when(message.getText()).thenReturn(jsonAiResponse);

        org.springframework.ai.chat.client.ChatClient.CallResponseSpec callSpec = mock(org.springframework.ai.chat.client.ChatClient.CallResponseSpec.class);
        when(chatClient.prompt().tools(any()).options(any(ChatOptions.Builder.class)).messages(any(), any()).call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(mockResponse);

        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1));

        assertNotNull(results);
        verify(aiAnalysisLogService, times(1)).saveLog(any());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando ChatClient não estiver disponível")
    void shouldFallbackWhenChatClientUnavailable() {
        when(chatClientBuilderProvider.getIfAvailable()).thenReturn(null);

        AnalisysFactoryNotebookImpl factory = new AnalisysFactoryNotebookImpl(chatClientBuilderProvider, objectMapper, aiAnalysisLogService, detailsTools);

        ScrapedListingDTO item1 = createListing("item-1", "Notebook", BigDecimal.valueOf(3000));
        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1));

        assertTrue(results.isEmpty());
    }

    private ScrapedListingDTO createListing(String id, String title, BigDecimal price) {
        return new ScrapedListingDTO(
                id,
                Vendor.OLX,
                id,
                title,
                price,
                null,
                "https://olx.com.br/" + id,
                "Descrição do " + title,
                "SP",
                "São Paulo",
                "Centro",
                true,
                "ENTREGA",
                List.of(),
                "2026-08-27T10:00:00",
                "2026-08-27T10:00:00"
        );
    }
}
