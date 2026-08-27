package br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record BatchAnalysisResponse(
    @JsonProperty("results") List<ItemAnalysisResult> results
) {}
