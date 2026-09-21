package br.dev.bielsolosos.biscraper.domain.monitoring.analysis.impl;

import com.ethlo.time.internal.util.ArrayUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.OpenAPI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.checkerframework.checker.units.qual.s;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import br.dev.bielsolosos.biscraper.core.abstractfields.NotebookAnalysisTypeFields;
import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.core.enums.DiskType;
import br.dev.bielsolosos.biscraper.core.enums.LlmModelEnum;
import br.dev.bielsolosos.biscraper.core.enums.NotebookBrand;
import br.dev.bielsolosos.biscraper.core.enums.ProcessorBrand;
import br.dev.bielsolosos.biscraper.core.enums.RamType;
import br.dev.bielsolosos.biscraper.core.enums.ScreenResolution;
import br.dev.bielsolosos.biscraper.domain.ai.model.dto.AiAnalysisLogCreateDto;
import br.dev.bielsolosos.biscraper.domain.ai.service.AiAnalysisLogService;
import br.dev.bielsolosos.biscraper.domain.ai.tools.ScrappingDetailsTools;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.AnalisysFactory;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.AnalisysResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.utils.AiAnalisysUtils;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapingExecution;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapedListingDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AnalisysFactoryNotebookImpl implements AnalisysFactory {

    private final OpenAPI customOpenAPI;
    private final ChatClient chatClient;
    private final AiAnalysisLogService aiAnalysisLogService;
    private final ScrappingDetailsTools detailsTools;
    private final ObjectMapper nullFallbackObjectmapper = new ObjectMapper()
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

    private final String systemPrompt = String.format(
            """
            Você é um agente especializado em analisar as buscas de equipamentos de computador. Especificamente Notebooks.
            Seu objetivo é extrair o máximo das informações do notebook. Você receberá do prompt uma lista de json com as informações de cada anuncio com o seguinte formato.

            {}

            Os esquema que vem agora de início se diz respeito somente a listagem da busca no site. Não contem informação alguma referente a descrição ou itens a mais dos sites que consultamos.
            Tendo isso em vista você tem registrado as TOOLS necessárias para buscar a descição ou a descrição com imagem. Busque sempre que for necessário para você avaliar melhor.
            Você precisa buscar: Marca do Notebook, Marca do processador, Geração do processador, quantidade de ram, tipo da ram (ddr4/ddr5/ddr3), tamanho do HD/SSD, se é um HD ou SSD, Resolução da tela e se tem GPU.
            Caso opte por imagens analise aqueles stickers que ficam no palmrest do notebook que pode se dizer a respeito sobre algumas das configurações do notebook que vieram de fábrica como por exemplo geração do processador, qualidade da tela, quantidade de ram e muitos outros itens possíveis.
            Utilize das tools o quanto precisar e caso veja que é necessário utilizar das imagens pode utilizar. Porém dê preferência para buscar somente a descrição do caso.

            A sua resposta tem que ser obrigatóriamente com um array desse tipo.

            {}

            Como você pode ver tem bastante enum para você preencher. Caso você não consiga extrair devolva como null ou desconhecido. O mapper vai transofrmar em nulo caso não seja nenhuma das possibilidades.
            """,
            AiAnalisysUtils.getJsonSchema(ScrapedListingDTO.class),
            AiAnalisysUtils.getJsonSchema(AiExtractedItem.class));

    private record AiExtractedItem(
            NotebookBrand brand,
            ProcessorBrand processorBrand,
            Integer processGeneration,
            Integer ramSize,
            RamType ramType,
            Integer storageSizeGb,
            DiskType diskType,
            ScreenResolution screenResolution,
            Boolean hasGpu
    ) {}

    public AnalisysFactoryNotebookImpl(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            ObjectMapper objectMapper,
            AiAnalysisLogService aiAnalysisLogService,
            ScrappingDetailsTools detailsTools, OpenAPI customOpenAPI) {
        this.aiAnalysisLogService = aiAnalysisLogService;
        this.chatClient = initChatClient(chatClientBuilderProvider);
        this.detailsTools = detailsTools;
        this.customOpenAPI = customOpenAPI;
    }

    @Override
    public AnalysisType getAnalisysType() {
        return AnalysisType.NOTEBOOK;
    }

    @Override
    public List<AnalisysResponse> analizeScrappedItens(ScrapingExecution execution, List<ScrapedListingDTO> listings) {
        if (listings == null || listings.isEmpty()) {
            log.debug("Lista de anúncios vazia ou nula. Nenhuma análise de notebook necessária.");
            return Collections.emptyList();
        }

        ProductMonitor monitor = execution != null ? execution.getProductMonitor() : null;
        log.info("Iniciando análise especializada de NOTEBOOK via Gemini para {} anúncios do monitor '{}'.",
                listings.size(), monitor != null ? monitor.getName() : "N/A");

        if (!isChatClientAvailable(execution, listings.size())) {
            return Collections.emptyList();
        }

        String itemsJson = AiAnalisysUtils.formatBatchForPrompt(listings);
        String modelName = LlmModelEnum.GEMINI_2_5_FLASH_LITE.getModel();
        long timerStart = System.currentTimeMillis();

        try {
            log.debug("Disparando prompt de extração com Tools para {} anúncios usando modelo '{}'...",
                    listings.size(), modelName);

            ChatResponse response = chatClient.prompt()
                    .tools(detailsTools)
                    .options(ChatOptions.builder()
                            .model(modelName)
                            .temperature(0.2))
                    .messages(new SystemMessage(systemPrompt), new UserMessage(itemsJson))
                    .call()
                    .chatResponse();

            long duration = System.currentTimeMillis() - timerStart;
            log.info("Análise LLM de notebooks concluída com sucesso para {} anúncios em {}ms.",
                    listings.size(), duration);

            String rawResponse = response != null && response.getResult() != null && response.getResult().getOutput() != null
                    ? response.getResult().getOutput().getText()
                    : "";

            // Log de Auditoria
            AiAnalysisLogCreateDto dtoBuilder = AiAnalysisLogCreateDto.fromResponse(response)
                    .productMonitor(monitor)
                    .scrapingExecution(execution)
                    .modelName(modelName)
                    .itemsCount(listings.size())
                    .systemPrompt(systemPrompt)
                    .userPrompt(itemsJson)
                    .rawResponse(rawResponse)
                    .status("SUCCESS")
                    .durationMs((int) duration)
                    .build();

            aiAnalysisLogService.saveLog(dtoBuilder);

            AiExtractedItem[] itensToAnalyze = this.nullFallbackObjectmapper.readValue(
                    AiAnalisysUtils.sanitizeJson(rawResponse),
                    AiExtractedItem[].class);

            List<AnalisysResponse> itensAnalized = new ArrayList<>();        

            for (AiExtractedItem currentAnalisys : itensToAnalyze) {
                AnalisysResponse itemAnalized = analyzeExtractedItems(currentAnalisys, execution, monitor);

                if (itemAnalized == null) continue;

                itensAnalized.add(itemAnalized);
            }        
            
            return itensAnalized;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - timerStart;
            log.error("Erro na análise de notebooks via Gemini: {}. Gravando log de erro e aplicando fallback.",
                    e.getMessage(), e);

            AiAnalysisLogCreateDto errorDto = AiAnalysisLogCreateDto.builder()
                    .productMonitor(monitor)
                    .scrapingExecution(execution)
                    .modelName(modelName)
                    .itemsCount(listings.size())
                    .systemPrompt(systemPrompt)
                    .userPrompt(itemsJson)
                    .status("ERROR")
                    .durationMs((int) duration)
                    .errorMessage("Não foi possível analisar utilizando IA. " + e.getMessage())
                    .build();

            aiAnalysisLogService.saveLog(errorDto);

            if (execution != null) {
                execution.setUsedFallback(true);
            }
            return Collections.emptyList();
        }
    }



    private AnalisysResponse analyzeExtractedItems(AiExtractedItem currentAnalisys, ScrapingExecution execution, ProductMonitor monitor) {
        // Começa com 100 e vai perdendo. 
        int score = 100;
        NotebookAnalysisTypeFields fieldsForAnalise = new NotebookAnalysisTypeFields(monitor.getExpectedSpecs());

        if (fieldsForAnalise.)

    }

    private boolean isChatClientAvailable(ScrapingExecution execution, int listingsCount) {
        if (this.chatClient == null) {
            log.warn("ChatClient (Spring AI / Gemini) não disponível no contexto. Aplicando fallback (MatchTier.NONE) para {} anúncios.",
                    listingsCount);
            if (execution != null) {
                execution.setUsedFallback(true);
            }
            return false;
        }
        return true;
    }
    private ChatClient initChatClient(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        return builder != null ? builder.build() : null;
    }
}
