package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.webhook;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapeResponseDTO;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookIncomingPayload(
    @JsonProperty("requestId")
    @JsonAlias("request_id")
    String requestId,

    @JsonProperty("jobId")
    @JsonAlias("job_id")
    String jobId,

    String status,

    ScrapeResponseDTO response
) {}
