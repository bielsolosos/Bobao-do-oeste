package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

public record OverviewStatsDto(
        long totalListings,
        long highRelevanceCount,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Double avgPrice
) {
    public MetricsOverviewResponse toResponse(OffsetDateTime lastScrapedAt) {
        BigDecimal formattedAvgPrice = (avgPrice != null)
                ? BigDecimal.valueOf(avgPrice).setScale(2, RoundingMode.HALF_UP)
                : null;
        return new MetricsOverviewResponse(
                totalListings,
                highRelevanceCount,
                minPrice,
                maxPrice,
                formattedAvgPrice,
                lastScrapedAt
        );
    }
}
