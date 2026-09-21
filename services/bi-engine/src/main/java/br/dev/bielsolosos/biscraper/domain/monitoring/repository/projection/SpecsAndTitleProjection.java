package br.dev.bielsolosos.biscraper.domain.monitoring.repository.projection;

import java.util.Map;

public interface SpecsAndTitleProjection {
    Map<String, Object> getExtractedSpecs();
    String getTitle();
}
