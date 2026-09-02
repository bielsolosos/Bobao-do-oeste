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
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.core.enums.LlmModelEnum;
import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.domain.ai.model.dto.AiAnalysisLogCreateDto;
import br.dev.bielsolosos.biscraper.domain.ai.tools.ScrappingDetailsTools;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.AnalisysFactory;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.AnalisysResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.dto.BatchAnalysisResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.dto.ItemAnalysisResult;
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
            Sua missão é realizar uma triagem inteligente e coletar informações adicionais APENAS quando estritamente necessário para validar se um anúncio atende ao critério do usuário.

            CRITÉRIO DO USUÁRIO:
            \"\"\"
            {userCriteria}
            \"\"\"

            REGRAS GERAIS DE TRIAGEM E USO DE FERRAMENTAS:

            1. Você possui acesso a ferramentas auxiliares (Tools) que podem enriquecer a sua investigação.
            2. LEIA ATENTAMENTE a descrição de cada ferramenta antes de usá-la. As regras exatas de QUANDO e COMO usar (ou não usar) cada ferramenta estão documentadas na própria descrição delas. Você DEVE respeitá-las rigorosamente.
            3. Como princípio de ouro: NUNCA acione ferramentas para produtos obviamente incompatíveis com o critério do usuário (marcas, categorias ou gerações erradas) ou que possuam defeitos graves que os desclassifiquem imediatamente.
            4. Se o título e os dados básicos fornecidos na entrada inicial já contiverem todas as informações essenciais necessárias para 100%% de validação, confie nesses dados e poupe as chamadas às ferramentas.

            SAÍDA DESTA ETAPA:
            - NÃO calcule nem atribua notas de 0 a 100.
            - Para cada anúncio (identificado por vendor_listing_id), compile um dossiê técnico conciso detalhando: identificação confirmada (marca/modelo/geração), especificações completas apuradas e o veredito se o item é um candidato válido ou foi descartado.
            """;

    private static final String EVALUATION_SYSTEM_TEMPLATE = """
            Você é um especialista em inteligência de compras e juiz avaliador de mercado (hardware, peças de computador, videogames, eletrônicos em geral).
            Sua função é avaliar detalhadamente cada anúncio com base no dossiê técnico investigado e nos critérios e preferências do usuário, atribuindo a pontuação final e gerando o resultado estruturado.

            CRITÉRIO DO USUÁRIO:
            \"\"\"
            {userCriteria}
            \"\"\"

            DIRETRIZES DE AVALIAÇÃO E PONTUAÇÃO:
            1. Avalie cada anúncio com um score numérico de 0.00 a 100.00:
               - 85.00 a 100.00: Excelente oportunidade, atende perfeitamente aos requisitos e preferências do usuário (especificações corretas, modelo desejado, bom estado).
               - 65.00 a 84.99: Boa oportunidade, atende aos requisitos principais com pequenas ressalvas aceitáveis (ex: variante ligeiramente diferente mas compatível, marcas de uso).
               - 45.00 a 64.99: Parcialmente aderente ou com ressalvas moderadas.
               - 0.00 a 44.99: Não relevante, produto incorreto, geração incompatível, fora do escopo ou preço abusivo.
            2. Para cada anúncio (vendor_listing_id), forneça:
               - score: nota de 0.00 a 100.00.
               - summary: justificativa técnica concisa e objetiva explicando a nota.
               - highlights: lista de diferenciais e pontos positivos.
               - concerns: lista de limitações, ressalvas ou pontos de atenção.
            3. Retorne a resposta estritamente no formato BatchAnalysisResponse.
            """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final br.dev.bielsolosos.biscraper.domain.ai.service.AiAnalysisLogService aiAnalysisLogService;
    private final ScrappingDetailsTools detailsTools;

    public AnalisysFactorySimpleImpl(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            ObjectMapper objectMapper,
            br.dev.bielsolosos.biscraper.domain.ai.service.AiAnalysisLogService aiAnalysisLogService,
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
        String itemsJson = formatBatchForPrompt(batch);
        ProductMonitor monitor = execution != null ? execution.getProductMonitor() : null;
        String modelName = LlmModelEnum.GEMINI_2_5_FLASH_LITE.getModel();

        // ==============================================================================
        // ETAPA 1: Investigação e Coleta de Dados via Tool Calling (Sem cálculo de notas)
        // ==============================================================================
        long step1Start = System.currentTimeMillis();
        String enrichedAnalysis;

        try {
            log.debug("Executando Etapa 1 (Investigação e Coleta com Tools) para lote de {} anúncios...", batch.size());
            ChatResponse response = chatClient.prompt()
                    .tools(detailsTools)
                    .options(ChatOptions.builder()
                            .model(modelName)
                            .temperature(0.2))
                    .system(s -> s.text(ENRICHMENT_SYSTEM_TEMPLATE).param("userCriteria", userCriteria))
                    .user(u -> u.text("Analise os anúncios a seguir e obtenha mais informações via ferramenta quando necessário para montar o dossiê de cada um:\n{itemsJson}").param("itemsJson", itemsJson))
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
                    .systemPrompt(ENRICHMENT_SYSTEM_TEMPLATE.replace("{userCriteria}", userCriteria))
                    .userPrompt("Investigação de " + batch.size() + " anúncios:\n" + itemsJson)
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
                    .systemPrompt(ENRICHMENT_SYSTEM_TEMPLATE.replace("{userCriteria}", userCriteria))
                    .userPrompt(itemsJson)
                    .status("ERROR")
                    .durationMs(step1Duration)
                    .errorMessage("Etapa 1 (Investigação) falhou: " + e.getMessage())
                    .build();

            aiAnalysisLogService.saveLog(errorDto);

            if (execution != null) execution.setUsedFallback(true);
            return createFallbackResponses(execution, batch);
        }

        // ==============================================================================
        // ETAPA 2: Avaliação, Pontuação e Estruturação Estrita no DTO (BatchAnalysisResponse)
        // ==============================================================================
        long step2Start = System.currentTimeMillis();

        try {
            log.debug("Executando Etapa 2 (Avaliação e Estruturação de Objeto) para lote de {} anúncios...", batch.size());
            BeanOutputConverter<BatchAnalysisResponse> converter = new BeanOutputConverter<>(BatchAnalysisResponse.class);

            ChatResponse response2 = chatClient.prompt()
                    .options(ChatOptions.builder()
                            .model(modelName)
                            .temperature(0.1))
                    .system(s -> s.text(EVALUATION_SYSTEM_TEMPLATE).param("userCriteria", userCriteria))
                    .user(u -> u.text("Avalie os seguintes anúncios com base no dossiê técnico investigado e gere o resultado estruturado:\n\n{enrichedAnalysis}\n\n{format}")
                                .param("enrichedAnalysis", enrichedAnalysis != null ? enrichedAnalysis : "")
                                .param("format", converter.getFormat()))
                    .call()
                    .chatResponse();

            String responseContent = response2.getResult().getOutput().getText();
            BatchAnalysisResponse aiResponse = converter.convert(responseContent);

            int step2Duration = (int) (System.currentTimeMillis() - step2Start);

            // Log de Auditoria da Etapa 2
            AiAnalysisLogCreateDto dtoBuilder = AiAnalysisLogCreateDto.fromResponse(response2)
                    .productMonitor(monitor)
                    .scrapingExecution(execution)
                    .modelName(modelName)
                    .itemsCount(batch.size())
                    .systemPrompt(EVALUATION_SYSTEM_TEMPLATE.replace("{userCriteria}", userCriteria))
                    .userPrompt(enrichedAnalysis != null ? enrichedAnalysis : "")
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
                    params.put("highlights", result.highlights() != null ? result.highlights() : Collections.emptyList());
                    params.put("concerns", result.concerns() != null ? result.concerns() : Collections.emptyList());
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
                    .systemPrompt(EVALUATION_SYSTEM_TEMPLATE.replace("{userCriteria}", userCriteria))
                    .userPrompt(enrichedAnalysis != null ? enrichedAnalysis : "")
                    .status("ERROR")
                    .durationMs(step2Duration)
                    .errorMessage("Etapa 2 (Avaliação) falhou: " + e.getMessage())
                    .build();
            aiAnalysisLogService.saveLog(errorDto);

            if (execution != null) execution.setUsedFallback(true);
            return createFallbackResponses(execution, batch);
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

        if (monitor.getExpectedSpecs() != null && monitor.getExpectedSpecs().has("prompt")) {
            String prompt = monitor.getExpectedSpecs().get("prompt").asText();
            if (prompt != null && !prompt.isBlank()) {
                return prompt;
            }
        }

        StringBuilder sb = new StringBuilder();
        if (monitor.getName() != null) sb.append("Produto: ").append(monitor.getName()).append(". ");
        if (monitor.getDescription() != null) sb.append("Detalhes: ").append(monitor.getDescription()).append(".");
        return sb.length() > 0 ? sb.toString() : "Avalie a relevância e o custo-benefício geral do produto.";
    }

    /**
     * Método responsável por transformar todos os itens coletados em um json para a IA ler.
     * @param batch
     * @return
     */
    private String formatBatchForPrompt(List<ScrapedListingDTO> batch) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (ScrapedListingDTO item : batch) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("vendor_listing_id", item.vendorListingId());
            map.put("vendor", item.vendor() != null ? item.vendor().name() : "OLX");
            map.put("url", item.url());
            map.put("title", item.title());
            map.put("price", item.price());
            map.put("original_price", item.originalPrice());
            map.put("location", (item.city() != null ? item.city() : "") + (item.state() != null ? "/" + item.state() : ""));
            map.put("has_delivery", item.hasDelivery());
            map.put("description", item.description() != null && item.description().length() > 300
                    ? item.description().substring(0, 300) + "..."
                    : item.description());
            list.add(map);
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return list.toString();
        }
    }

    private List<AnalisysResponse> createFallbackResponses(ScrapingExecution execution, List<ScrapedListingDTO> listings) {
        return listings.stream()
                .map(item -> new AnalisysResponse(execution, item, MatchTier.NONE, BigDecimal.ZERO, Map.of()))
                .toList();
    }
}
