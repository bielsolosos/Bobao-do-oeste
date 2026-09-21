package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics;

public record TierMetricsResponse(
        long high,
        long medium,
        long low,
        long none,
        long total) {
}
