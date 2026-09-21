package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics;

import java.util.List;

public record BrandDistributionResponse(
    List<BrandItem> brands
) {
    public record BrandItem(
        String brand,
        long count
    ) {}
}
