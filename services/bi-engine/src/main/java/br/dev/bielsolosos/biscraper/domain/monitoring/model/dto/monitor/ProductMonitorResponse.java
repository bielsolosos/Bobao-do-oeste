package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.monitor;

import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.core.enums.ScrapingFrequency;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductMonitorResponse(
    UUID id,
    String name,
    String description,
    AnalysisType analysisType,
    Vendor targetVendor,
    boolean active,
    boolean requiredDelivery,
    ScrapingFrequency frequency,
    String cronExpression,
    Map<String, Object> expectedSpecs,
    List<MonitorSearchQueryResponse> searchQueries,
    OffsetDateTime lastScrapedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
