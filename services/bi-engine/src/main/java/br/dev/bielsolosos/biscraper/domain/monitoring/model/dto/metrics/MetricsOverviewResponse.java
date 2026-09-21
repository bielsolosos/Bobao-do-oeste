package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MetricsOverviewResponse(
        long totalListings,
        long highRelevanceCount,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal avgPrice,
        OffsetDateTime lastScrapedAt) {
}
