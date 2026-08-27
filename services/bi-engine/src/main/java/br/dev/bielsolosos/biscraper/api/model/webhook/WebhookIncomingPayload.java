package br.dev.bielsolosos.biscraper.api.model.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record WebhookIncomingPayload(
    @JsonProperty("request_id")
    String requestId,

    @JsonProperty("job_id")
    String jobId,

    String status,

    Map<String, Object> response
) {}
