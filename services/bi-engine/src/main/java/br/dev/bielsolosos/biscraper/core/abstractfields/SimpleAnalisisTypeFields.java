package br.dev.bielsolosos.biscraper.core.abstractfields;

import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.Map;

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
        return prompt != null ? Map.of("prompt", prompt) : Map.of();
    }
}
