package br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AsyncScrapeClientResponse(
    @JsonProperty("request_id")
    String requestId,
    @JsonProperty("job_id")
    String jobId,
    String status,
    @JsonProperty("webhook_url")
    String webhookUrl
) {}
