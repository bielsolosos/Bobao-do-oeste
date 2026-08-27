package br.dev.bielsolosos.biscraper.api.model.productmonitor.monitorfields;

import br.dev.bielsolosos.biscraper.core.abstractfields.AnalysisTypeFields;
import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class NotebookAnalysisTypeFields extends AnalysisTypeFields {

    @Min(value = 4, message = "Memória RAM mínima deve ser de pelo menos 4GB.")
    private Integer minimumRamGb;

    private Boolean needsDedicatedGpu;
    private String requiredProcessor;
    private String requiredStorage;

    @Override
    public AnalysisType getAnalysisType() {
        return AnalysisType.NOTEBOOK;
    }

    @Override
    public Map<String, Object> getFields() {
        Map<String, Object> fields = new HashMap<>();
        if (minimumRamGb != null) fields.put("minimumRamGb", minimumRamGb);
        if (needsDedicatedGpu != null) fields.put("needsDedicatedGpu", needsDedicatedGpu);
        if (requiredProcessor != null) fields.put("requiredProcessor", requiredProcessor);
        if (requiredStorage != null) fields.put("requiredStorage", requiredStorage);
        return fields;
    }
}
