package br.dev.bielsolosos.biscraper.api.model.webhook;

import br.dev.bielsolosos.biscraper.core.enums.WebhookStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WebhookEventSummaryResponse(
    UUID id,
    String eventType,
    String requestId,
    String jobId,
    WebhookStatus status,
    OffsetDateTime processedAt,
    OffsetDateTime createdAt
) {}
