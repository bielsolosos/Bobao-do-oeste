package br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.dto;

import br.dev.bielsolosos.biscraper.core.enums.DiskType;
import br.dev.bielsolosos.biscraper.core.enums.NotebookBrand;
import br.dev.bielsolosos.biscraper.core.enums.ProcessorBrand;
import br.dev.bielsolosos.biscraper.core.enums.ProcessorTier;
import br.dev.bielsolosos.biscraper.core.enums.RamType;
import br.dev.bielsolosos.biscraper.core.enums.ScreenResolution;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Representa os dados técnicos extraídos e inferidos do notebook após a análise.
 * Este objeto é convertido e persistido na coluna JSONB (extracted_specs) da entidade ScrapedListing,
 * permitindo que a API e o Front-end realizem filtros e ordenações diretas por especificações de hardware.
 */
public record NotebookExtractedSpecsDto(
        NotebookBrand brand,
        ProcessorBrand processorBrand,
        String processorModel,
        Integer processGeneration,
        ProcessorTier processorTier,
        Integer ramSize,
        RamType ramType,
        Integer storageSizeGb,
        DiskType diskType,
        ScreenResolution screenResolution,
        Boolean hasGpu,
        Boolean isReproved,
        List<String> evaluationNotes
) {

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (brand != null) map.put("brand", brand.name());
        if (processorBrand != null) map.put("processorBrand", processorBrand.name());
        if (processorModel != null && !processorModel.isBlank()) map.put("processorModel", processorModel);
        if (processGeneration != null) map.put("processGeneration", processGeneration);
        if (processorTier != null) map.put("processorTier", processorTier.name());
        if (ramSize != null) map.put("ramSize", ramSize);
        if (ramType != null) map.put("ramType", ramType.name());
        if (storageSizeGb != null) map.put("storageSizeGb", storageSizeGb);
        if (diskType != null) map.put("diskType", diskType.name());
        if (screenResolution != null) map.put("screenResolution", screenResolution.name());
        if (hasGpu != null) map.put("hasGpu", hasGpu);
        map.put("isReproved", isReproved != null && isReproved);
        if (evaluationNotes != null && !evaluationNotes.isEmpty()) {
            map.put("evaluationNotes", evaluationNotes);
        }
        return map;
    }
}
