package br.dev.bielsolosos.biscraper.domain.monitoring.analysis.impl;

import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.core.enums.LlmModelEnum;
import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.AnalisysFactory;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.AnalisysResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.dto.BatchAnalysisResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.dto.ItemAnalysisResult;
import br.dev.bielsolosos.biscraper.domain.ai.model.AiAnalysisLog;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapingExecution;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapedListingDTO;
import br.dev.bielsolosos.biscraper.domain.ai.repository.AiAnalysisLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AnalisysFactorySimpleImpl implements AnalisysFactory {

    private static final int BATCH_SIZE = 15;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final AiAnalysisLogRepository aiAnalysisLogRepository;

    public AnalisysFactorySimpleImpl(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            ObjectMapper objectMapper,
            AiAnalysisLogRepository aiAnalysisLogRepository
    ) {
        this.objectMapper = objectMapper;
        this.aiAnalysisLogRepository = aiAnalysisLogRepository;
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        this.chatClient = builder != null ? builder.build() : null;
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
        long startTime = System.currentTimeMillis();
        String itemsJson = formatBatchForPrompt(batch);
        ProductMonitor monitor = execution != null ? execution.getProductMonitor() : null;
        String modelName = LlmModelEnum.GEMINI_2_5_FLASH_LITE.getModel();

        String systemPrompt = """
                Você é um especialista em inteligência de compras e análise de mercado para marketplaces (OLX, Mercado Livre, etc.).
                Sua função é avaliar cada anúncio recebido em relação aos critérios e preferências descritos pelo usuário.

                CRITÉRIO DO USUÁRIO:
                \"\"\"
                %s
                \"\"\"

                INSTRUÇÕES:
                1. Avalie cada anúncio com um score numérico de 0.00 a 100.00:
                   - 85 a 100: Excelente oportunidade, atende a todos ou praticamente todos os requisitos descritos.
                   - 65 a 84.99: Boa oportunidade, atende aos requisitos principais com pequenas divergências aceitáveis.
                   - 45 a 64.99: Parcialmente aderente ou anúncio vago.
                   - 0 a 44.99: Não relevante, produto incorreto, fora do escopo ou preço abusivo.
                2. Forneça um resumo conciso (summary) explicando a avaliação.
                3. Liste pontos positivos (highlights) e pontos de atenção/negativos (concerns).
                4. Retorne a resposta estritamente no formato JSON compatível com BatchAnalysisResponse.
                """.formatted(userCriteria);

        String userPrompt = "Analise os seguintes anúncios:\n" + itemsJson;

        try {
            ChatOptions options = ChatOptions.builder()
                    .model(modelName)
                    .temperature(0.2)
                    .build();

            BatchAnalysisResponse aiResponse = chatClient.prompt()
                    .options(options)
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .entity(BatchAnalysisResponse.class);

            int durationMs = (int) (System.currentTimeMillis() - startTime);

            // Grava o log de auditoria da chamada no banco
            logAiCall(monitor, execution, modelName, batch.size(), systemPrompt, userPrompt,
                    objectMapper.writeValueAsString(aiResponse), "SUCCESS", durationMs, null);

            if (aiResponse == null || aiResponse.results() == null || aiResponse.results().isEmpty()) {
                log.warn("Gemini retornou resposta vazia para o lote de {} itens. Aplicando fallback.", batch.size());
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
            int durationMs = (int) (System.currentTimeMillis() - startTime);
            log.error("Erro ao analisar lote de anúncios com Gemini: {}. Gravando log de erro e aplicando fallback.", e.getMessage(), e);

            logAiCall(monitor, execution, modelName, batch.size(), systemPrompt, userPrompt,
                    null, "ERROR", durationMs, e.getMessage());

            if (execution != null) execution.setUsedFallback(true);
            return createFallbackResponses(execution, batch);
        }
    }

    private void logAiCall(
            ProductMonitor monitor,
            ScrapingExecution execution,
            String modelName,
            int itemsCount,
            String systemPrompt,
            String userPrompt,
            String rawResponse,
            String status,
            Integer durationMs,
            String errorMessage
    ) {
        try {
            AiAnalysisLog aiLog = AiAnalysisLog.builder()
                    .productMonitor(monitor)
                    .scrapingExecution(execution)
                    .modelName(modelName)
                    .vendor("GEMINI")
                    .itemsCount(itemsCount)
                    .systemPrompt(systemPrompt)
                    .userPrompt(userPrompt)
                    .rawResponse(rawResponse)
                    .status(status)
                    .durationMs(durationMs)
                    .errorMessage(errorMessage)
                    .build();

            aiAnalysisLogRepository.save(aiLog);
        } catch (Exception ex) {
            log.warn("Falha ao salvar AiAnalysisLog no banco: {}", ex.getMessage());
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
