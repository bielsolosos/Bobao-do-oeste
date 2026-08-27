package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScrapeResponseDTO(
    boolean success,
    ExecutionSummaryDTO execution,
    List<ScrapedListingDTO> items
) {}
