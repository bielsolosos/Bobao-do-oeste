package br.dev.bielsolosos.biscraper.domain.ai.model.dto;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapingExecution;
import lombok.Builder;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.Usage;

@Builder
public record AiAnalysisLogCreateDto(
        ProductMonitor productMonitor,
        ScrapingExecution scrapingExecution,
        String requestId,
        String jobId,
        String modelName,
        String vendor,
        int itemsCount,
        String systemPrompt,
        String userPrompt,
        String rawResponse,
        String status,
        Integer durationMs,
        String errorMessage,
        Integer promptTokens,
        Integer generationTokens,
        Integer totalTokens
) {
    public static AiAnalysisLogCreateDtoBuilder fromResponse(ChatResponse response) {
        AiAnalysisLogCreateDtoBuilder builder = AiAnalysisLogCreateDto.builder();
        if (response != null && response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            Usage usage = response.getMetadata().getUsage();
            builder.promptTokens(usage.getPromptTokens() != null ? usage.getPromptTokens().intValue() : null)
                   .generationTokens(usage.getCompletionTokens() != null ? usage.getCompletionTokens().intValue() : null)
                   .totalTokens(usage.getTotalTokens() != null ? usage.getTotalTokens().intValue() : null);
        }
        return builder;
    }
}
