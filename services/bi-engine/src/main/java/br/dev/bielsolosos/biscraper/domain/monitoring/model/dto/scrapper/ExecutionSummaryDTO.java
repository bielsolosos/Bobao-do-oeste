package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper;

import br.dev.bielsolosos.biscraper.core.config.FlexibleOffsetDateTimeDeserializer;
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
    @JsonProperty("started_at") String startedAt,
    @JsonProperty("finished_at") String finishedAt
) {
    public OffsetDateTime getParsedStartedAt() {
        return FlexibleOffsetDateTimeDeserializer.parse(this.startedAt);
    }

    public OffsetDateTime getParsedFinishedAt() {
        return FlexibleOffsetDateTimeDeserializer.parse(this.finishedAt);
    }
}
