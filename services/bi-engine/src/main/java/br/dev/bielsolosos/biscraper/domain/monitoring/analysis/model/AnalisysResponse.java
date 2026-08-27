package br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model;

import java.math.BigDecimal;
import java.util.Map;

import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapingExecution;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapedListingDTO;

public record AnalisysResponse(
    ScrapingExecution execution,
    ScrapedListingDTO listing,
    MatchTier matchTier,
    BigDecimal matchScore,
    Map<String, Object> params) {

}
