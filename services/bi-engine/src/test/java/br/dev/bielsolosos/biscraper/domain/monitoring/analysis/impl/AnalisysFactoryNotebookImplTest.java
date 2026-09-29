package br.dev.bielsolosos.biscraper.domain.monitoring.analysis.impl;

import br.dev.bielsolosos.biscraper.core.config.AiChatClientFactory;
import br.dev.bielsolosos.biscraper.core.enums.*;
import br.dev.bielsolosos.biscraper.domain.ai.service.AiAnalysisLogService;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.ScraperHttpClient;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.AnalisysResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapingExecution;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapedListingDTO;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.model.UserConfig;
import br.dev.bielsolosos.biscraper.domain.users.service.UserConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalisysFactoryNotebookImplTest {

    @Mock
    private AiChatClientFactory aiChatClientFactory;

    @Mock
    private UserConfigService userConfigService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock
    private AiAnalysisLogService aiAnalysisLogService;

    @Mock
    private ScraperHttpClient scraperClient;

    @InjectMocks
    private AnalisysFactoryNotebookImpl factory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ScrapingExecution execution;
    private ProductMonitor monitor;
    private User user;
    private UserConfig userConfig;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).username("testuser").build();

        userConfig = UserConfig.builder()
                .user(user)
                .aiVendor(ModelVendorEnum.GEMINI)
                .cheapModel("gemini-2.5-flash")
                .strongModel("gemini-2.5-pro")
                .build();

        monitor = new ProductMonitor();
        monitor.setId(UUID.randomUUID());
        monitor.setName("Dell i7 16GB");
        monitor.setUser(user);

        execution = ScrapingExecution.builder()
                .id(UUID.randomUUID())
                .productMonitor(monitor)
                .build();

        lenient().when(userConfigService.getConfigForUser(user)).thenReturn(userConfig);
        lenient().when(aiChatClientFactory.getChatClient(ModelVendorEnum.GEMINI)).thenReturn(chatClient);
    }

    @Test
    @DisplayName("Deve retornar AnalysisType.NOTEBOOK")
    void shouldReturnCorrectAnalysisType() {
        assertEquals(AnalysisType.NOTEBOOK, factory.getAnalisysType());
    }

    @Test
    @DisplayName("Deve invocar Gemini e converter resposta para DTOs de notebook com match HIGH")
    void shouldAnalyzeNotebookListingsSuccessfully() {
        monitor.setExpectedSpecs(Map.of(
                "processorVendors", List.of("INTEL"),
                "processorTiers", List.of("ADVANCED"),
                "minimumProcessorGeneration", 11
        ));

        ScrapedListingDTO item1 = createListing("item-1", "Dell G15 i7 11800H 16GB SSD 512 RTX 3050 Full HD", BigDecimal.valueOf(3800));

        String jsonAiResponse = """
                [
                    {
                        "brand": "DELL",
                        "processorBrand": "INTEL",
                        "processorModel": "Core i7-11800H",
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
        when(chatClient.prompt().options(any(ChatOptions.Builder.class)).messages(any(), any()).call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(mockResponse);

        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1));

        assertNotNull(results);
        assertEquals(1, results.size());
        AnalisysResponse response = results.getFirst();
        assertEquals(MatchTier.HIGH, response.matchTier());
        assertEquals(BigDecimal.valueOf(100), response.matchScore());
        assertEquals("INTEL", response.params().get("processorBrand"));
        assertEquals("ADVANCED", response.params().get("processorTier"));
        assertEquals(false, response.params().get("isReproved"));

        verify(aiAnalysisLogService, times(1)).saveLog(any());
    }

    @Test
    @DisplayName("Deve reprovar notebook quando fabricante do processador for incompatível")
    void shouldReproveWhenProcessorBrandIncompatible() {
        monitor.setExpectedSpecs(Map.of(
                "processorVendors", List.of("INTEL")
        ));

        ScrapedListingDTO item1 = createListing("item-1", "Notebook AMD Ryzen 5 5500U", BigDecimal.valueOf(2500));

        String jsonAiResponse = """
                [
                    {
                        "brand": "LENOVO",
                        "processorBrand": "AMD",
                        "processorModel": "Ryzen 5 5500U",
                        "processGeneration": 5,
                        "ramSize": 8
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
        when(chatClient.prompt().options(any(ChatOptions.Builder.class)).messages(any(), any()).call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(mockResponse);

        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1));

        assertEquals(1, results.size());
        AnalisysResponse response = results.getFirst();
        assertEquals(MatchTier.NONE, response.matchTier());
        assertEquals(BigDecimal.ZERO, response.matchScore());
        assertEquals(true, response.params().get("isReproved"));
    }

    @Test
    @DisplayName("Deve penalizar nota quando geração for 1 nível abaixo do mínimo exigido")
    void shouldPenalizeWhenGenerationSlightlyBelow() {
        monitor.setExpectedSpecs(Map.of(
                "minimumProcessorGeneration", 11
        ));

        ScrapedListingDTO item1 = createListing("item-1", "Dell i5 10210U 8GB", BigDecimal.valueOf(2000));

        String jsonAiResponse = """
                [
                    {
                        "brand": "DELL",
                        "processorBrand": "INTEL",
                        "processorModel": "Core i5 10210U",
                        "processGeneration": 10,
                        "ramSize": 8
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
        when(chatClient.prompt().options(any(ChatOptions.Builder.class)).messages(any(), any()).call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(mockResponse);

        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1));

        assertEquals(1, results.size());
        AnalisysResponse response = results.getFirst();
        assertEquals(MatchTier.HIGH, response.matchTier()); // 90 pts >= 85 (HIGH)
        assertEquals(BigDecimal.valueOf(90), response.matchScore());
    }

    @Test
    @DisplayName("Deve penalizar nota quando memória RAM for abaixo da mínima desejada (ex: 8GB quando pediu 16GB)")
    void shouldPenalizeWhenRamBelowMinimum() {
        monitor.setExpectedSpecs(Map.of(
                "minimumRamGb", 16
        ));

        ScrapedListingDTO item1 = createListing("item-1", "Notebook Dell i5 8GB", BigDecimal.valueOf(2200));

        String jsonAiResponse = """
                [
                    {
                        "brand": "DELL",
                        "processorBrand": "INTEL",
                        "processorModel": "Core i5-1135G7",
                        "processGeneration": 11,
                        "ramSize": 8,
                        "ramType": "DDR4"
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
        when(chatClient.prompt().options(any(ChatOptions.Builder.class)).messages(any(), any()).call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(mockResponse);

        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1));

        assertEquals(1, results.size());
        AnalisysResponse response = results.getFirst();
        assertEquals(MatchTier.MEDIUM, response.matchTier()); // 80 pts (100 - 20)
        assertEquals(BigDecimal.valueOf(80), response.matchScore());
    }

    @Test
    @DisplayName("Deve reprovar anúncio quando memória RAM for 4GB e o usuário exigiu 16GB+")
    void shouldReproveWhenRamCriticallyBelowMinimum() {
        monitor.setExpectedSpecs(Map.of(
                "minimumRamGb", 16
        ));

        ScrapedListingDTO item1 = createListing("item-1", "Notebook Básico 4GB", BigDecimal.valueOf(1200));

        String jsonAiResponse = """
                [
                    {
                        "brand": "SAMSUNG",
                        "processorBrand": "INTEL",
                        "processorModel": "Celeron",
                        "ramSize": 4
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
        when(chatClient.prompt().options(any(ChatOptions.Builder.class)).messages(any(), any()).call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(mockResponse);

        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1));

        assertEquals(1, results.size());
        AnalisysResponse response = results.getFirst();
        assertEquals(MatchTier.NONE, response.matchTier());
        assertEquals(BigDecimal.ZERO, response.matchScore());
        assertEquals(true, response.params().get("isReproved"));
    }

    @Test
    @DisplayName("Deve penalizar quando tecnologia de RAM for divergente da desejada (DDR4 em vez de DDR5)")
    void shouldPenalizeWhenRamTypeDivergent() {
        monitor.setExpectedSpecs(Map.of(
                "ramTypes", List.of("DDR5", "LPDDR5")
        ));

        ScrapedListingDTO item1 = createListing("item-1", "Dell 16GB DDR4", BigDecimal.valueOf(2800));

        String jsonAiResponse = """
                [
                    {
                        "brand": "DELL",
                        "processorBrand": "INTEL",
                        "processorModel": "Core i7-11800H",
                        "ramSize": 16,
                        "ramType": "DDR4"
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
        when(chatClient.prompt().options(any(ChatOptions.Builder.class)).messages(any(), any()).call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(mockResponse);

        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1));

        assertEquals(1, results.size());
        AnalisysResponse response = results.getFirst();
        assertEquals(MatchTier.HIGH, response.matchTier()); // 85 pts (100 - 15) >= 85 (HIGH)
        assertEquals(BigDecimal.valueOf(85), response.matchScore());
    }

    @Test
    @DisplayName("Deve penalizar nota quando armazenamento for 256GB e o usuário pediu 512GB")
    void shouldPenalizeWhenStorageBelowMinimum() {
        monitor.setExpectedSpecs(Map.of(
                "minimumStorageGb", 512
        ));

        ScrapedListingDTO item1 = createListing("item-1", "Dell i5 256GB SSD", BigDecimal.valueOf(2500));

        String jsonAiResponse = """
                [
                    {
                        "brand": "DELL",
                        "processorBrand": "INTEL",
                        "processorModel": "Core i5-1135G7",
                        "storageSizeGb": 256,
                        "diskType": "SSD_NVME"
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
        when(chatClient.prompt().options(any(ChatOptions.Builder.class)).messages(any(), any()).call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(mockResponse);

        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1));

        assertEquals(1, results.size());
        AnalisysResponse response = results.getFirst();
        assertEquals(MatchTier.HIGH, response.matchTier()); // 85 pts (100 - 15) >= 85 (HIGH)
        assertEquals(BigDecimal.valueOf(85), response.matchScore());
    }

    @Test
    @DisplayName("Deve reprovar notebook quando armazenamento for 64GB e o usuário exigiu 512GB+")
    void shouldReproveWhenStorageCriticallyBelowMinimum() {
        monitor.setExpectedSpecs(Map.of(
                "minimumStorageGb", 512
        ));

        ScrapedListingDTO item1 = createListing("item-1", "Notebook Positivo 64GB", BigDecimal.valueOf(800));

        String jsonAiResponse = """
                [
                    {
                        "brand": "OTHER",
                        "processorBrand": "INTEL",
                        "processorModel": "Celeron N4020",
                        "storageSizeGb": 64,
                        "diskType": "EMMC"
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
        when(chatClient.prompt().options(any(ChatOptions.Builder.class)).messages(any(), any()).call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(mockResponse);

        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1));

        assertEquals(1, results.size());
        AnalisysResponse response = results.getFirst();
        assertEquals(MatchTier.NONE, response.matchTier());
        assertEquals(BigDecimal.ZERO, response.matchScore());
        assertEquals(true, response.params().get("isReproved"));
    }

    @Test
    @DisplayName("Deve penalizar quando tecnologia de disco for HD Mecânico e o usuário exigiu SSD")
    void shouldPenalizeWhenHddInsteadOfSsd() {
        monitor.setExpectedSpecs(Map.of(
                "diskTypes", List.of("SSD_NVME", "SSD")
        ));

        ScrapedListingDTO item1 = createListing("item-1", "Dell 1TB HDD", BigDecimal.valueOf(1900));

        String jsonAiResponse = """
                [
                    {
                        "brand": "DELL",
                        "processorBrand": "INTEL",
                        "processorModel": "Core i5-1135G7",
                        "storageSizeGb": 1024,
                        "diskType": "HDD"
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
        when(chatClient.prompt().options(any(ChatOptions.Builder.class)).messages(any(), any()).call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(mockResponse);

        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1));

        assertEquals(1, results.size());
        AnalisysResponse response = results.getFirst();
        assertEquals(MatchTier.MEDIUM, response.matchTier()); // 70 pts (100 - 30)
        assertEquals(BigDecimal.valueOf(70), response.matchScore());
    }

    @Test
    @DisplayName("Deve reprovar notebook quando o usuário exigir GPU dedicada e o anúncio possuir apenas gráficos integrados")
    void shouldReproveWhenDedicatedGpuRequiredButIntegrated() {
        monitor.setExpectedSpecs(Map.of(
                "needsDedicatedGpu", true
        ));

        ScrapedListingDTO item1 = createListing("item-1", "Dell Inspiron Intel Iris Xe", BigDecimal.valueOf(3200));

        String jsonAiResponse = """
                [
                    {
                        "brand": "DELL",
                        "processorBrand": "INTEL",
                        "processorModel": "Core i7-1165G7",
                        "hasGpu": false
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
        when(chatClient.prompt().options(any(ChatOptions.Builder.class)).messages(any(), any()).call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(mockResponse);

        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1));

        assertEquals(1, results.size());
        AnalisysResponse response = results.getFirst();
        assertEquals(MatchTier.NONE, response.matchTier());
        assertEquals(BigDecimal.ZERO, response.matchScore());
        assertEquals(true, response.params().get("isReproved"));
    }

    @Test
    @DisplayName("Deve penalizar quando resolução da tela for HD (720p) e o usuário exigiu Full HD+")
    void shouldPenalizeWhenScreenResolutionIsHd() {
        monitor.setExpectedSpecs(Map.of(
                "screenResolutions", List.of("FULL_HD", "QHD_2K")
        ));

        ScrapedListingDTO item1 = createListing("item-1", "Notebook Tela HD", BigDecimal.valueOf(2000));

        String jsonAiResponse = """
                [
                    {
                        "brand": "LENOVO",
                        "processorBrand": "INTEL",
                        "processorModel": "Core i5-1135G7",
                        "screenResolution": "HD"
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
        when(chatClient.prompt().options(any(ChatOptions.Builder.class)).messages(any(), any()).call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(mockResponse);

        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1));

        assertEquals(1, results.size());
        AnalisysResponse response = results.getFirst();
        assertEquals(MatchTier.HIGH, response.matchTier()); // 85 pts (100 - 15) >= 85 (HIGH)
        assertEquals(BigDecimal.valueOf(85), response.matchScore());
    }

    @Test
    @DisplayName("Deve reprovar notebook quando fabricante da marca for incompatível")
    void shouldReproveWhenBrandIncompatible() {
        monitor.setExpectedSpecs(Map.of(
                "brands", List.of("DELL", "LENOVO")
        ));

        ScrapedListingDTO item1 = createListing("item-1", "Notebook Acer Nitro", BigDecimal.valueOf(3500));

        String jsonAiResponse = """
                [
                    {
                        "brand": "ACER",
                        "processorBrand": "INTEL",
                        "processorModel": "Core i5-11400H"
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
        when(chatClient.prompt().options(any(ChatOptions.Builder.class)).messages(any(), any()).call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(mockResponse);

        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1));

        assertEquals(1, results.size());
        AnalisysResponse response = results.getFirst();
        assertEquals(MatchTier.NONE, response.matchTier());
        assertEquals(BigDecimal.ZERO, response.matchScore());
        assertEquals(true, response.params().get("isReproved"));
    }

    @Test
    @DisplayName("Deve extrair corretamente geração de diferentes processadores (Intel 11th-14th, Core Ultra, Ryzen, Apple M, texto explícito)")
    void shouldExtractGenerationAccuratelyForVariousProcessors() {
        monitor.setExpectedSpecs(Map.of(
                "minimumProcessorGeneration", 11
        ));

        ScrapedListingDTO item1 = createListing("item-1", "Dell Inspiron 3501 i5-1135G7 16GB", BigDecimal.valueOf(2800));
        ScrapedListingDTO item2 = createListing("item-2", "Lenovo IdeaPad Gaming 3 Ryzen 7 5700U 16GB", BigDecimal.valueOf(3200));
        ScrapedListingDTO item3 = createListing("item-3", "Asus TUF i7-13700H 16GB", BigDecimal.valueOf(4500));
        ScrapedListingDTO item4 = createListing("item-4", "Acer Swift Core Ultra 7 155H", BigDecimal.valueOf(5000));
        ScrapedListingDTO item5 = createListing("item-5", "Dell Inspiron 3501", BigDecimal.valueOf(2000));

        String jsonAiResponse = """
                [
                    {
                        "brand": "DELL",
                        "processorBrand": "INTEL",
                        "processorModel": "Core i5-1135G7",
                        "processGeneration": null,
                        "ramSize": 16
                    },
                    {
                        "brand": "LENOVO",
                        "processorBrand": "AMD",
                        "processorModel": "Ryzen 7 5700U",
                        "processGeneration": null,
                        "ramSize": 16
                    },
                    {
                        "brand": "ASUS",
                        "processorBrand": "INTEL",
                        "processorModel": "Core i7-13700H",
                        "processGeneration": null,
                        "ramSize": 16
                    },
                    {
                        "brand": "ACER",
                        "processorBrand": "INTEL",
                        "processorModel": "Core Ultra 7 155H",
                        "processGeneration": null,
                        "ramSize": 16
                    },
                    {
                        "brand": "DELL",
                        "processorBrand": null,
                        "processorModel": "Inspiron 3501",
                        "processGeneration": 3501,
                        "ramSize": 8
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
        when(chatClient.prompt().options(any(ChatOptions.Builder.class)).messages(any(), any()).call()).thenReturn(callSpec);
        when(callSpec.chatResponse()).thenReturn(mockResponse);

        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1, item2, item3, item4, item5));

        assertEquals(5, results.size());

        // Item 1: i5-1135G7 -> Gen 11
        assertEquals(11, results.get(0).params().get("processGeneration"));
        assertEquals("INTERMEDIATE", results.get(0).params().get("processorTier"));

        // Item 2: Ryzen 7 5700U -> Gen 5
        assertEquals(5, results.get(1).params().get("processGeneration"));
        assertEquals("ADVANCED", results.get(1).params().get("processorTier"));

        // Item 3: i7-13700H -> Gen 13
        assertEquals(13, results.get(2).params().get("processGeneration"));
        assertEquals("ADVANCED", results.get(2).params().get("processorTier"));

        // Item 4: Ultra 7 155H -> Gen 1
        assertEquals(1, results.get(3).params().get("processGeneration"));
        assertEquals("ADVANCED", results.get(3).params().get("processorTier"));

        // Item 5: Dell Inspiron 3501 com explicitGen 3501 -> descartado como ruído de chassi (null)
        assertNull(results.get(4).params().get("processGeneration"));
        assertNull(results.get(4).params().get("processorTier"));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando ChatClient não estiver disponível")
    void shouldFallbackWhenChatClientUnavailable() {
        when(aiChatClientFactory.getChatClient(ModelVendorEnum.GEMINI)).thenReturn(null);

        ScrapedListingDTO item1 = createListing("item-1", "Notebook", BigDecimal.valueOf(3000));
        List<AnalisysResponse> results = factory.analizeScrappedItens(execution, List.of(item1));

        assertTrue(results.isEmpty());
        assertTrue(execution.isUsedFallback());
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
