package br.dev.bielsolosos.biscraper.domain.monitoring.analysis.impl;

import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.AnalisysResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.dto.BatchAnalysisResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.dto.ItemAnalysisResult;
import br.dev.bielsolosos.biscraper.domain.ai.model.AiAnalysisLog;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapingExecution;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapedListingDTO;
import br.dev.bielsolosos.biscraper.domain.ai.repository.AiAnalysisLogRepository;
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

import java.lang.module.ModuleDescriptor.Builder;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalisysFactorySimpleImplTest {

    @Mock
    private ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient.Builder chatClientBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock
    private AiAnalysisLogRepository aiAnalysisLogRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ScrapingExecution execution;
    private ProductMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = ProductMonitor.builder()
                .id(UUID.randomUUID())
                .name("Notebook Gamer i7")
                .description("Procurando notebook i7 com 16gb de ram")
                .analysisType(AnalysisType.SIMPLE)
                .build();

        execution = ScrapingExecution.builder()
                .id(UUID.randomUUID())
                .productMonitor(monitor)
                .build();
    }

    @Test
    @DisplayName("Deve retornar AnalysisType.SIMPLE")
    void shouldReturnCorrectAnalysisType() {
        when(chatClientBuilderProvider.getIfAvailable()).thenReturn(null);
        AnalisysFactorySimpleImpl factory = new AnalisysFactorySimpleImpl(chatClientBuilderProvider, objectMapper, aiAnalysisLogRepository);
        assertEquals(AnalysisType.SIMPLE, factory.getAnalisysType());
    }

    @Test
    @DisplayName("Deve classificar anúncios em Tiers e salvar AiAnalysisLog quando Gemini responder com sucesso")
    void shouldClassifyItemsIntoTiersSuccessfully() {
        when(chatClientBuilderProvider.getIfAvailable()).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        AnalisysFactorySimpleImpl factory = new AnalisysFactorySimpleImpl(chatClientBuilderProvider, objectMapper, aiAnalysisLogRepository);

        ScrapedListingDTO item1 = createListing("item-1", "Dell G15 i7 16GB RTX 3050", BigDecimal.valueOf(3500));
        ScrapedListingDTO item2 = createListing("item-2", "Acer Nitro 5 i5 8GB", BigDecimal.valueOf(2800));
        ScrapedListingDTO item3 = createListing("item-3", "Positivo Celeron 4GB", BigDecimal.valueOf(800));

        ItemAnalysisResult res1 = new ItemAnalysisResult("item-1", BigDecimal.valueOf(95.0), "Excelente estado e atende todos requisitos", List.of("i7", "16GB"), List.of());
        ItemAnalysisResult res2 = new ItemAnalysisResult("item-2", BigDecimal.valueOf(70.0), "Bom notebook mas tem 8GB", List.of("Preço bom"), List.of("Apenas 8GB"));
        ItemAnalysisResult res3 = new ItemAnalysisResult("item-3", BigDecimal.valueOf(20.0), "Não atende especificações", List.of(), List.of("Celeron"));

        BatchAnalysisResponse aiResponse = new BatchAnalysisResponse(List.of(res1, res2, res3));

        when(chatClient.prompt()
                .options(any(ChatOptions.Builder.class))
                .system(anyString())
                .user(anyString())
                .call()
                .entity(BatchAnalysisResponse.class)).thenReturn(aiResponse);

        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1, item2, item3));

        assertEquals(3, results.size());

        // Item 1 -> HIGH (Score 95)
        assertEquals(MatchTier.HIGH, results.get(0).matchTier());
        assertEquals(BigDecimal.valueOf(95.0).setScale(2), results.get(0).matchScore());
        assertEquals("Excelente estado e atende todos requisitos", results.get(0).params().get("summary"));

        // Item 2 -> MEDIUM (Score 70)
        assertEquals(MatchTier.MEDIUM, results.get(1).matchTier());
        assertEquals(BigDecimal.valueOf(70.0).setScale(2), results.get(1).matchScore());

        // Item 3 -> NONE (Score 20)
        assertEquals(MatchTier.NONE, results.get(2).matchTier());
        assertEquals(BigDecimal.valueOf(20.0).setScale(2), results.get(2).matchScore());

        verify(aiAnalysisLogRepository, times(1)).save(any(AiAnalysisLog.class));
    }

    @Test
    @DisplayName("Deve aplicar fallback gracioso (MatchTier.NONE) quando ChatClient não estiver disponível")
    void shouldFallbackWhenChatClientUnavailable() {
        when(chatClientBuilderProvider.getIfAvailable()).thenReturn(null);

        AnalisysFactorySimpleImpl factory = new AnalisysFactorySimpleImpl(chatClientBuilderProvider, objectMapper, aiAnalysisLogRepository);

        ScrapedListingDTO item1 = createListing("item-1", "Notebook", BigDecimal.valueOf(3000));
        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1));

        assertEquals(1, results.size());
        assertEquals(MatchTier.NONE, results.get(0).matchTier());
        assertEquals(BigDecimal.ZERO, results.get(0).matchScore());
        assertTrue(execution.isUsedFallback());
    }

    @Test
    @DisplayName("Deve registrar log com status ERROR e aplicar fallback gracioso quando Gemini lançar exceção")
    void shouldFallbackWhenGeminiThrowsException() {
        when(chatClientBuilderProvider.getIfAvailable()).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        AnalisysFactorySimpleImpl factory = new AnalisysFactorySimpleImpl(chatClientBuilderProvider, objectMapper, aiAnalysisLogRepository);

        when(chatClient.prompt()
                .options(any(ChatOptions.Builder.class))
                .system(anyString())
                .user(anyString())
                .call()
                .entity(BatchAnalysisResponse.class)).thenThrow(new RuntimeException("Gemini quota 429 exceeded"));

        ScrapedListingDTO item1 = createListing("item-1", "Notebook", BigDecimal.valueOf(3000));
        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1));

        assertEquals(1, results.size());
        assertEquals(MatchTier.NONE, results.get(0).matchTier());
        assertEquals(BigDecimal.ZERO, results.get(0).matchScore());
        assertTrue(execution.isUsedFallback());

        verify(aiAnalysisLogRepository, times(1)).save(argThat(log -> "ERROR".equals(log.getStatus())));
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
