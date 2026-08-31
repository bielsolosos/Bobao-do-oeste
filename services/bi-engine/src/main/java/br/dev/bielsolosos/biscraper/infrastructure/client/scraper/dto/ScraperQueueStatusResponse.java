package br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ScraperQueueStatusResponse(
    @JsonProperty("queued_jobs") int queuedJobs,
    @JsonProperty("running_jobs") int runningJobs,
    @JsonProperty("total_pending_jobs") int totalPendingJobs,
    @JsonProperty("pending_webhooks") int pendingWebhooks,
    @JsonProperty("total_success_jobs") int totalSuccessJobs,
    @JsonProperty("total_failed_jobs") int totalFailedJobs
) {}
