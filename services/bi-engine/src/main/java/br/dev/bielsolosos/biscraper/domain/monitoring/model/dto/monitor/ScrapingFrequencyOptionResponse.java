package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.monitor;

import br.dev.bielsolosos.biscraper.core.enums.ScrapingFrequency;

public record ScrapingFrequencyOptionResponse(
        ScrapingFrequency name,
        String description,
        String cronExpression
) {
    public static ScrapingFrequencyOptionResponse from(ScrapingFrequency frequency) {
        return new ScrapingFrequencyOptionResponse(
                frequency,
                frequency.getDescription(),
                frequency.getCronExpression()
        );
    }
}
