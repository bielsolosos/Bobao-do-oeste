package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics;

import java.math.BigDecimal;
import java.util.List;

public record PriceDistributionResponse(
        List<PriceBucket> buckets) {
    public record PriceBucket(
            String label,
            BigDecimal min,
            BigDecimal max,
            long count) {
    }
}
