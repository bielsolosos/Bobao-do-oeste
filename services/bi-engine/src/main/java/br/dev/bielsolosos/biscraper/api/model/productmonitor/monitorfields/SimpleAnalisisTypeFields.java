package br.dev.bielsolosos.biscraper.api.model.productmonitor.monitorfields;

import java.util.Map;

import br.dev.bielsolosos.biscraper.core.abstractfields.AnalysisTypeFields;
import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Data
public class SimpleAnalisisTypeFields extends AnalysisTypeFields {

  private String prompt;

  @Override
  public AnalysisType getAnalysisType() {
    return AnalysisType.SIMPLE;
  }

  @Override
  public Map<String, Object> getFields() {
    return Map.of("prompt", this.prompt);
  }

}
