package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.monitor;

import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.core.enums.ScrapingFrequency;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProductMonitorResponse(
    UUID id,
    String name,
    String description,
    AnalysisType analysisType,
    Vendor targetVendor,
    boolean active,
    ScrapingFrequency frequency,
    String cronExpression,
    JsonNode expectedSpecs,
    List<MonitorSearchQueryResponse> searchQueries,
    OffsetDateTime lastScrapedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
