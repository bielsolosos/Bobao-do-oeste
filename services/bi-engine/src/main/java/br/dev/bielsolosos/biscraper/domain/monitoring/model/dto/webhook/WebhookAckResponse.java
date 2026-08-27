package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.webhook;

import java.time.OffsetDateTime;

public record WebhookAckResponse(
        String status,
        String requestId,
        OffsetDateTime timestamp
) {}
