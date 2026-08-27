package br.dev.bielsolosos.biscraper.domain.monitoring.analysis.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.AnalisysFactory;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.AnalisysResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapingExecution;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapedListingDTO;

@Component
public class AnalisysFactoryNoneImpl implements AnalisysFactory {

  @Override
  public AnalysisType getAnalisysType() {
    return AnalysisType.NONE;
  }

  @Override
  public List<AnalisysResponse> analizeScrappedItens(ScrapingExecution execution, List<ScrapedListingDTO> listings) {
    return listings.stream()
        .map(item -> new AnalisysResponse(execution, item, MatchTier.NONE, BigDecimal.ZERO, Map.of())).toList();
  }

}
