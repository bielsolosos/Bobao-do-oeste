package br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AsyncScrapeClientRequest(
    ScrapeJobRequest request,
    @JsonProperty("webhookUrl")
    String webhookUrl
) {}
