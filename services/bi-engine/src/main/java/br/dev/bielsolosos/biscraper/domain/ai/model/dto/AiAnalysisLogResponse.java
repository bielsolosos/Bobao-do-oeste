package br.dev.bielsolosos.biscraper.domain.ai.model.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AiAnalysisLogResponse(
    UUID id,
    UUID productMonitorId,
    String productMonitorName,
    UUID scrapingExecutionId,
    String modelName,
    String vendor,
    int itemsCount,
    String systemPrompt,
    String userPrompt,
    String rawResponse,
    String status,
    Integer durationMs,
    Integer promptTokens,
    Integer generationTokens,
    Integer totalTokens,
    String errorMessage,
    OffsetDateTime createdAt
) {}
