package br.dev.bielsolosos.biscraper.api.mapper.ai;

import br.dev.bielsolosos.biscraper.domain.ai.model.AiAnalysisLog;
import br.dev.bielsolosos.biscraper.domain.ai.model.dto.AiAnalysisLogResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AiAnalysisLogMapper {

    public static AiAnalysisLogResponse toResponse(AiAnalysisLog log) {
        if (log == null) {
            return null;
        }

        String requestId = log.getRequestId();
        if (requestId == null && log.getScrapingExecution() != null && log.getScrapingExecution().getWebhookEvent() != null) {
            requestId = log.getScrapingExecution().getWebhookEvent().getRequestId();
        }

        String jobId = log.getJobId();
        if (jobId == null && log.getScrapingExecution() != null && log.getScrapingExecution().getWebhookEvent() != null) {
            jobId = log.getScrapingExecution().getWebhookEvent().getJobId();
        }

        return new AiAnalysisLogResponse(
                log.getId(),
                log.getProductMonitor() != null ? log.getProductMonitor().getId() : null,
                log.getProductMonitor() != null ? log.getProductMonitor().getName() : null,
                log.getScrapingExecution() != null ? log.getScrapingExecution().getId() : null,
                requestId,
                jobId,
                log.getModelName(),
                log.getVendor(),
                log.getItemsCount(),
                log.getSystemPrompt(),
                log.getUserPrompt(),
                log.getRawResponse(),
                log.getStatus(),
                log.getDurationMs(),
                log.getPromptTokens(),
                log.getGenerationTokens(),
                log.getTotalTokens(),
                log.getErrorMessage(),
                log.getCreatedAt()
        );
    }
}
