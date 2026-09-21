package br.dev.bielsolosos.biscraper.domain.monitoring.analysis.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.core.enums.LlmModelEnum;
import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.domain.ai.model.dto.AiAnalysisLogCreateDto;
import br.dev.bielsolosos.biscraper.domain.ai.service.AiAnalysisLogService;
import br.dev.bielsolosos.biscraper.domain.ai.tools.ScrappingDetailsTools;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.AnalisysFactory;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.AnalisysResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.dto.BatchAnalysisResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.dto.ItemAnalysisResult;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.utils.AiAnalisysUtils;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapingExecution;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapedListingDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AnalisysFactorySimpleImpl implements AnalisysFactory {

    private static final int BATCH_SIZE = 15;

    private static final String ENRICHMENT_SYSTEM_TEMPLATE = """
            Você é um especialista em investigação técnica de produtos em marketplaces.
            Sua missão é realizar triagem e coletar informações adicionais com ferramentas (Tools) para validar se anúncios atendem ao critério do usuário.

            CRITÉRIO DO USUÁRIO:
            \"\"\"
            {userCriteria}
            \"\"\"

            REGRAS DE INVESTIGAÇÃO E USO DE FERRAMENTAS (TOOLS):
            1. Descarte IMEDIATAMENTE produtos obviamente incompatíveis (marcas, modelos ou categorias erradas) SEM acionar ferramentas.
            2. Para anúncios que são candidatos válidos/promissores mas possuem dados resumidos incompletos ou ambíguos (ex: memória RAM, capacidade SSD, variante exata do chip, versão do modelo ou estado de conservação), ACIONE as ferramentas 'getAdditionalInfo' ou 'getAdditionalInfoAndImages' passando o 'vendor' e a 'url' do item.
            3. Se os dados fornecidos no resumo já forem suficientes para confirmar se o item atende aos critérios com 100% de certeza, não é necessário acionar ferramentas.

            SAÍDA DESTA ETAPA:
            - NÃO calcule nem atribua notas numéricas de 0 a 100 nesta etapa.
            - Para cada anúncio (identificado por vendor_listing_id), monte um dossiê técnico objetivo com status (DESCARTADO / INVESTIGADO / VALIDADO) e as especificações técnicas apuradas (máx 2 linhas por anúncio).
            """;

    private static final String EVALUATION_SYSTEM_TEMPLATE = """
            Você é um juiz avaliador de compras de produtos em marketplaces.
            Avalie os anúncios com base no dossiê técnico investigado e nos critérios do usuário, atribuindo nota e justificativa curta.

            CRITÉRIO DO USUÁRIO:
            \"\"\"
            {userCriteria}
            \"\"\"

            DIRETRIZES DE PONTUAÇÃO (0.0 a 100.0):
            - 85.0 a 100.0: Excelente oportunidade (atende plenamente aos requisitos e bom preço).
            - 65.0 a 84.9: Boa oportunidade com pequenas ressalvas aceitáveis.
            - 45.0 a 64.9: Parcialmente aderente ou ressalvas moderadas.
            - 0.0 a 44.9: Não relevante, produto incorreto, fora do escopo ou defeituoso.

            DIRETRIZES DE SAÍDA:
            - "reason": Justificativa técnica estritamente concisa em no máximo 15 palavras.
            - Responda EXCLUSIVAMENTE em JSON válido com a estrutura:
            {"results":[{"id":"ID_DO_ANUNCIO","score":85.0,"reason":"justificativa curta"}]}
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final AiAnalysisLogService aiAnalysisLogService;
    private final ScrappingDetailsTools detailsTools;

    public AnalisysFactorySimpleImpl(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            ObjectMapper objectMapper,
            AiAnalysisLogService aiAnalysisLogService,
            ScrappingDetailsTools detailsTools
    ) {
        this.objectMapper = objectMapper;
        this.aiAnalysisLogService = aiAnalysisLogService;
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        this.chatClient = builder != null ? builder.build() : null;
        this.detailsTools = detailsTools;
    }

    @Override
    public AnalysisType getAnalisysType() {
        return AnalysisType.SIMPLE;
    }

    @Override
    public List<AnalisysResponse> analizeScrappedItens(ScrapingExecution execution, List<ScrapedListingDTO> listings) {
        if (listings == null || listings.isEmpty()) {
            return Collections.emptyList();
        }

        ProductMonitor monitor = execution != null ? execution.getProductMonitor() : null;
        String userCriteria = extractUserCriteria(monitor);

        log.info("Iniciando análise SIMPLE via Gemini para {} anúncios do monitor '{}'.",
                listings.size(), monitor != null ? monitor.getName() : "N/A");

        if (chatClient == null) {
            log.warn("ChatClient (Gemini) não disponível. Aplicando fallback gracioso (MatchTier.NONE) para {} itens.", listings.size());
            if (execution != null) execution.setUsedFallback(true);
            return createFallbackResponses(execution, listings);
        }

        List<AnalisysResponse> responses = new ArrayList<>(listings.size());

        // Particiona em lotes de até BATCH_SIZE (15) para otimização de custo e rate limit
        for (int i = 0; i < listings.size(); i += BATCH_SIZE) {
            List<ScrapedListingDTO> batch = listings.subList(i, Math.min(i + BATCH_SIZE, listings.size()));
            responses.addAll(analyzeBatch(execution, batch, userCriteria));
        }

        return responses;
    }

    private List<AnalisysResponse> analyzeBatch(
            ScrapingExecution execution,
            List<ScrapedListingDTO> batch,
            String userCriteria
    ) {
        String itemsJson = AiAnalisysUtils.formatBatchForPrompt(batch);
        ProductMonitor monitor = execution != null ? execution.getProductMonitor() : null;
        String modelName = LlmModelEnum.GEMINI_2_5_FLASH_LITE.getModel();

        // ==============================================================================
        // ETAPA 1: Investigação e Coleta de Dados via Tool Calling (Saída telegráfica enxuta)
        // ==============================================================================
        long step1Start = System.currentTimeMillis();
        String enrichedAnalysis;
        String step1SystemPrompt = ENRICHMENT_SYSTEM_TEMPLATE.replace("{userCriteria}", userCriteria);
        String step1UserPrompt = "Analise os anúncios a seguir e investigue via ferramentas os anúncios promissores que precisarem de confirmação técnica para montar o dossiê:\n" + itemsJson;

        try {
            log.debug("Executando Etapa 1 (Investigação e Coleta com Tools) para lote de {} anúncios...", batch.size());
            ChatResponse response = chatClient.prompt()
                    .tools(detailsTools)
                    .options(ChatOptions.builder()
                            .model(modelName)
                            .temperature(0.2))
                    .messages(new SystemMessage(step1SystemPrompt), new UserMessage(step1UserPrompt))
                    .call()
                    .chatResponse();

            enrichedAnalysis = response.getResult().getOutput().getText();
            int step1Duration = (int) (System.currentTimeMillis() - step1Start);

            // Log de Auditoria da Etapa 1
            AiAnalysisLogCreateDto dtoBuilder = AiAnalysisLogCreateDto.fromResponse(response)
                    .productMonitor(monitor)
                    .scrapingExecution(execution)
                    .modelName(modelName)
                    .itemsCount(batch.size())
                    .systemPrompt(step1SystemPrompt)
                    .userPrompt(step1UserPrompt)
                    .rawResponse(enrichedAnalysis)
                    .status("SUCCESS")
                    .durationMs(step1Duration)
                    .build();

            aiAnalysisLogService.saveLog(dtoBuilder);

        } catch (Exception e) {
            int step1Duration = (int) (System.currentTimeMillis() - step1Start);
            log.error("Erro na Etapa 1 (Investigação com Gemini): {}. Gravando log de erro e aplicando fallback.", e.getMessage(), e);

            AiAnalysisLogCreateDto errorDto = AiAnalysisLogCreateDto.builder()
                    .productMonitor(monitor)
                    .scrapingExecution(execution)
                    .modelName(modelName)
                    .itemsCount(batch.size())
                    .systemPrompt(step1SystemPrompt)
                    .userPrompt(step1UserPrompt)
                    .status("ERROR")
                    .durationMs(step1Duration)
                    .errorMessage("Etapa 1 (Investigação) falhou: " + e.getMessage())
                    .build();

            aiAnalysisLogService.saveLog(errorDto);

            if (execution != null) execution.setUsedFallback(true);
            return createFallbackResponses(execution, batch);
        }

        // ==============================================================================
        // ETAPA 2: Avaliação, Pontuação e Estruturação Enxuta (JSON com id, score, reason)
        // ==============================================================================
        long step2Start = System.currentTimeMillis();
        String step2SystemPrompt = EVALUATION_SYSTEM_TEMPLATE.replace("{userCriteria}", userCriteria);
        String step2UserPrompt = "Gere a avaliação JSON com base no dossiê técnico:\n\n" + (enrichedAnalysis != null ? enrichedAnalysis : "");

        try {
            log.debug("Executando Etapa 2 (Avaliação e Estruturação Enxuta) para lote de {} anúncios...", batch.size());

            ChatResponse response2 = chatClient.prompt()
                    .options(ChatOptions.builder()
                            .model(modelName)
                            .temperature(0.1))
                    .messages(new SystemMessage(step2SystemPrompt), new UserMessage(step2UserPrompt))
                    .call()
                    .chatResponse();

            String responseContent = response2.getResult().getOutput().getText();
            BatchAnalysisResponse aiResponse = parseBatchResponse(responseContent);

            int step2Duration = (int) (System.currentTimeMillis() - step2Start);

            // Log de Auditoria da Etapa 2
            AiAnalysisLogCreateDto dtoBuilder = AiAnalysisLogCreateDto.fromResponse(response2)
                    .productMonitor(monitor)
                    .scrapingExecution(execution)
                    .modelName(modelName)
                    .itemsCount(batch.size())
                    .systemPrompt(step2SystemPrompt)
                    .userPrompt(step2UserPrompt)
                    .rawResponse(responseContent)
                    .status("SUCCESS")
                    .durationMs(step2Duration)
                    .build();
                    
            aiAnalysisLogService.saveLog(dtoBuilder);

            if (aiResponse == null || aiResponse.results() == null || aiResponse.results().isEmpty()) {
                log.warn("Gemini retornou resposta vazia para o lote de {} itens na Etapa 2. Aplicando fallback.", batch.size());
                if (execution != null) execution.setUsedFallback(true);
                return createFallbackResponses(execution, batch);
            }

            Map<String, ItemAnalysisResult> resultMap = aiResponse.results().stream()
                    .filter(r -> r.vendorListingId() != null)
                    .collect(Collectors.toMap(
                            ItemAnalysisResult::vendorListingId,
                            r -> r,
                            (existing, replacement) -> existing
                    ));

            List<AnalisysResponse> batchResponses = new ArrayList<>();
            for (ScrapedListingDTO item : batch) {
                ItemAnalysisResult result = resultMap.get(item.vendorListingId());
                if (result != null && result.score() != null) {
                    BigDecimal score = result.score().setScale(2, RoundingMode.HALF_UP);
                    MatchTier tier = calculateTier(score);

                    Map<String, Object> params = new HashMap<>();
                    params.put("summary", result.summary() != null ? result.summary() : "");
                    params.put("score", score);

                    batchResponses.add(new AnalisysResponse(execution, item, tier, score, params));
                } else {
                    batchResponses.add(new AnalisysResponse(execution, item, MatchTier.NONE, BigDecimal.ZERO, Map.of()));
                }
            }

            return batchResponses;

        } catch (Exception e) {
            int step2Duration = (int) (System.currentTimeMillis() - step2Start);
            log.error("Erro na Etapa 2 (Avaliação com Gemini): {}. Aplicando fallback.", e.getMessage(), e);

            var errorDto = br.dev.bielsolosos.biscraper.domain.ai.model.dto.AiAnalysisLogCreateDto.builder()
                    .productMonitor(monitor)
                    .scrapingExecution(execution)
                    .modelName(modelName)
                    .itemsCount(batch.size())
                    .systemPrompt(step2SystemPrompt)
                    .userPrompt(step2UserPrompt)
                    .status("ERROR")
                    .durationMs(step2Duration)
                    .errorMessage("Etapa 2 (Avaliação) falhou: " + e.getMessage())
                    .build();
            aiAnalysisLogService.saveLog(errorDto);

            if (execution != null) execution.setUsedFallback(true);
            return createFallbackResponses(execution, batch);
        }
    }

    private BatchAnalysisResponse parseBatchResponse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return null;
        }
        try {
            String cleaned = AiAnalisysUtils.sanitizeJson(rawContent);
            return objectMapper.readValue(cleaned, BatchAnalysisResponse.class);
        } catch (Exception e) {
            log.warn("Falha ao desserializar JSON da Etapa 2: {}. Conteúdo bruto: {}", e.getMessage(), rawContent);
            return null;
        }
    }

    private MatchTier calculateTier(BigDecimal score) {
        if (score == null) return MatchTier.NONE;
        double val = score.doubleValue();
        if (val >= 85.0) return MatchTier.HIGH;
        if (val >= 65.0) return MatchTier.MEDIUM;
        if (val >= 45.0) return MatchTier.LOW;
        return MatchTier.NONE;
    }

    private String extractUserCriteria(ProductMonitor monitor) {
        if (monitor == null) return "Avalie a relevância e o custo-benefício geral do produto.";

        if (monitor.getExpectedSpecs() != null && monitor.getExpectedSpecs().containsKey("prompt")) {
            Object promptObj = monitor.getExpectedSpecs().get("prompt");
            if (promptObj != null && !promptObj.toString().isBlank()) {
                return promptObj.toString();
            }
        }

        StringBuilder sb = new StringBuilder();
        if (monitor.getName() != null) sb.append("Produto: ").append(monitor.getName()).append(". ");
        if (monitor.getDescription() != null) sb.append("Detalhes: ").append(monitor.getDescription()).append(".");
        return sb.length() > 0 ? sb.toString() : "Avalie a relevância e o custo-benefício geral do produto.";
    }

    private List<AnalisysResponse> createFallbackResponses(ScrapingExecution execution, List<ScrapedListingDTO> listings) {
        return listings.stream()
                .map(item -> new AnalisysResponse(execution, item, MatchTier.NONE, BigDecimal.ZERO, Map.of()))
                .toList();
    }
}
