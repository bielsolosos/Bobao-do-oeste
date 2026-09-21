package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics;

import java.math.BigDecimal;
import java.util.List;

public record PriceBucketCountsDto(
        long under1500,
        long between1500And3000,
        long between3000And5000,
        long between5000And8000,
        long above8000
) {
    public PriceDistributionResponse toResponse() {
        return new PriceDistributionResponse(List.of(
                new PriceDistributionResponse.PriceBucket("Até R$ 1.500", BigDecimal.ZERO, BigDecimal.valueOf(1500), under1500),
                new PriceDistributionResponse.PriceBucket("R$ 1.500 - R$ 3.000", BigDecimal.valueOf(1500), BigDecimal.valueOf(3000), between1500And3000),
                new PriceDistributionResponse.PriceBucket("R$ 3.000 - R$ 5.000", BigDecimal.valueOf(3000), BigDecimal.valueOf(5000), between3000And5000),
                new PriceDistributionResponse.PriceBucket("R$ 5.000 - R$ 8.000", BigDecimal.valueOf(5000), BigDecimal.valueOf(8000), between5000And8000),
                new PriceDistributionResponse.PriceBucket("Acima de R$ 8.000", BigDecimal.valueOf(8000), null, above8000)
        ));
    }

    public static PriceBucketCountsDto empty() {
        return new PriceBucketCountsDto(0, 0, 0, 0, 0);
    }
}
