package br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AsyncScrapeClientRequest(
    ScrapeJobRequest request,
    @JsonProperty("webhookUrl")
    String webhookUrl,
    @JsonProperty("requestId")
    String requestId
) {}
