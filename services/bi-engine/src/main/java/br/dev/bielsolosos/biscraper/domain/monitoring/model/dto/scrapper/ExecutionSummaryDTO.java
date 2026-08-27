package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper;

import br.dev.bielsolosos.biscraper.core.enums.ExecutionStatus;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExecutionSummaryDTO(
    String id,
    Vendor vendor,
    ExecutionStatus status,
    @JsonProperty("duration_ms") Integer durationMs,
    @JsonProperty("total_found") int totalFound,
    @JsonProperty("new_items_count") int newItemsCount,
    @JsonProperty("used_fallback") boolean usedFallback,
    @JsonProperty("error_message") String errorMessage,
    @JsonProperty("started_at") OffsetDateTime startedAt,
    @JsonProperty("finished_at") OffsetDateTime finishedAt
) {}
