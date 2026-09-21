package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics;

import java.util.List;

public record TimelineMetricsResponse(
        List<TimelinePoint> points) {
    public record TimelinePoint(
            String date,
            long count) {
    }
}
