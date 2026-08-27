package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record WebhookAckResponse(
    String status,
    @JsonProperty("requestId")
    String requestId,
    OffsetDateTime timestamp
) {}
