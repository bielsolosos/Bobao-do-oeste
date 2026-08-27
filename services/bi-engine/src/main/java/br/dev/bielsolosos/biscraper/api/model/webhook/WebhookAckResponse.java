package br.dev.bielsolosos.biscraper.api.model.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record WebhookAckResponse(
    String status,
    @JsonProperty("requestId")
    String requestId,
    OffsetDateTime timestamp
) {}
