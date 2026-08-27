package br.dev.bielsolosos.biscraper.domain.monitoring.analysis;

import java.util.List;

import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.AnalisysResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapingExecution;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapedListingDTO;

public interface AnalisysFactory {
  AnalysisType getAnalisysType();

  List<AnalisysResponse> analizeScrappedItens(ScrapingExecution execution, List<ScrapedListingDTO> listings);

}
