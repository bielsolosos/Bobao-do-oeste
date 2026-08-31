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

        return new AiAnalysisLogResponse(
                log.getId(),
                log.getProductMonitor() != null ? log.getProductMonitor().getId() : null,
                log.getProductMonitor() != null ? log.getProductMonitor().getName() : null,
                log.getScrapingExecution() != null ? log.getScrapingExecution().getId() : null,
                log.getModelName(),
                log.getVendor(),
                log.getItemsCount(),
                log.getSystemPrompt(),
                log.getUserPrompt(),
                log.getRawResponse(),
                log.getStatus(),
                log.getDurationMs(),
                log.getErrorMessage(),
                log.getCreatedAt()
        );
    }
}
