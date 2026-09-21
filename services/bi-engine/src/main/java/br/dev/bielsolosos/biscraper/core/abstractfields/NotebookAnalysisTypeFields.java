package br.dev.bielsolosos.biscraper.core.abstractfields;

import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.core.enums.DiskType;
import br.dev.bielsolosos.biscraper.core.enums.NotebookBrand;
import br.dev.bielsolosos.biscraper.core.enums.ProcessorBrand;
import br.dev.bielsolosos.biscraper.core.enums.ProcessorTier;
import br.dev.bielsolosos.biscraper.core.enums.RamType;
import br.dev.bielsolosos.biscraper.core.enums.ScreenResolution;
import lombok.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Representa os campos e critérios estruturados de monitoramento especializado para Notebooks.
 * 
 * DESIGN DO FORMULÁRIO (UX & IA):
 * - Todos os critérios são baseados em seleções múltiplas (arrays/chips) e filtros intuitivos,
 *   evitando campos abertos de texto livre que causam ambiguidades na análise pela IA.
 * - O processador é parametrizado por Fabricante (ProcessorBrand) + Nível de Força (ProcessorTier)
 *   + Geração Mínima opcional, garantindo precisão técnica sem exigir do usuário o conhecimento
 *   de dezenas de códigos complexos de CPUs (ex: i7-1165G7 vs Ryzen 5 5600H).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class NotebookAnalysisTypeFields extends AnalysisTypeFields {

    /**
     * Marcas de notebook aceitas (Seleção múltipla).
     * Ex: [APPLE, DELL, LENOVO]
     */
    private List<NotebookBrand> brands;

    /**
     * Fabricantes de processador aceitos (Seleção múltipla).
     * Ex: [INTEL, AMD, APPLE, QUALCOMM]
     */
    private List<ProcessorBrand> processorVendors;

    /**
     * Níveis de desempenho de processamento aceitos (Seleção múltipla).
     * - ENTRY: Básico (i3, Ryzen 3, N100, Celeron)
     * - INTERMEDIATE: Intermediário / Trabalho (i5, Ryzen 5, M1/M2/M3 base, Ultra 5)
     * - ADVANCED: Alto Desempenho / Gamer / Edição (i7, i9, Ryzen 7/9, M Pro/Max/Ultra, Ultra 7/9)
     */
    private List<ProcessorTier> processorTiers;

    /**
     * Geração mínima do processador para arquiteturas x86 Intel/AMD (Opcional).
     * Ex: 11 (para Intel 11ª geração ou AMD Ryzen série 5000+).
     */
    private Integer minimumProcessorGeneration;

    /**
     * Capacidade mínima de memória RAM em Gigabytes (Chips de seleção).
     * Ex: 8, 16, 32, 64 GB.
     */
    private Integer minimumRamGb;

    /**
     * Gerações/Tipos de memória RAM aceitos (Seleção múltipla).
     * Ex: [DDR4, DDR5, LPDDR5]
     */
    private List<RamType> ramTypes;

    /**
     * Capacidade mínima de armazenamento em Gigabytes (Chips de seleção).
     * Ex: 256, 512, 1024 (1TB), 2048 (2TB).
     */
    private Integer minimumStorageGb;

    /**
     * Tecnologias de disco aceitas (Seleção múltipla).
     * Ex: [SSD, SSD_NVME]
     */
    private List<DiskType> diskTypes;

    /**
     * Resoluções de tela desejadas (Seleção múltipla).
     * Ex: [FULL_HD, QHD_2K, RETINA]
     */
    private List<ScreenResolution> screenResolutions;

    /**
     * Exigência de placa de vídeo dedicada (GPU).
     * True: Exige GPU dedicada (NVIDIA GeForce / AMD Radeon).
     * False / Null: Aceita gráficos integrados ou Apple Silicon.
     */
    private Boolean needsDedicatedGpu;


    
    public NotebookAnalysisTypeFields(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            NotebookAnalysisTypeFields temp = mapper.convertValue(map, NotebookAnalysisTypeFields.class);

            this.brands = temp.getBrands();
            this.processorVendors = temp.getProcessorVendors();
            this.processorTiers = temp.getProcessorTiers();
            this.minimumProcessorGeneration = temp.getMinimumProcessorGeneration();
            this.minimumRamGb = temp.getMinimumRamGb();
            this.ramTypes = temp.getRamTypes();
            this.minimumStorageGb = temp.getMinimumStorageGb();
            this.diskTypes = temp.getDiskTypes();
            this.screenResolutions = temp.getScreenResolutions();
            this.needsDedicatedGpu = temp.getNeedsDedicatedGpu();

        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao converter Map para NotebookAnalysisTypeFields", e);
        }
    }

    public NotebookAnalysisTypeFields(JsonNode json) {
        if (json == null || json.isMissingNode() || json.isNull()) {
            return;
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            NotebookAnalysisTypeFields temp = mapper.treeToValue(json, NotebookAnalysisTypeFields.class);

            // Copia os valores convertidos para a instância atual
            this.brands = temp.getBrands();
            this.processorVendors = temp.getProcessorVendors();
            this.processorTiers = temp.getProcessorTiers();
            this.minimumProcessorGeneration = temp.getMinimumProcessorGeneration();
            this.minimumRamGb = temp.getMinimumRamGb();
            this.ramTypes = temp.getRamTypes();
            this.minimumStorageGb = temp.getMinimumStorageGb();
            this.diskTypes = temp.getDiskTypes();
            this.screenResolutions = temp.getScreenResolutions();
            this.needsDedicatedGpu = temp.getNeedsDedicatedGpu();

        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao converter JsonNode para NotebookAnalysisTypeFields", e);
        }
    }
    
    @Override
    public AnalysisType getAnalysisType() {
        return AnalysisType.NOTEBOOK;
    }

    @Override
    public Map<String, Object> getFields() {
        Map<String, Object> fields = new HashMap<>();
        if (brands != null && !brands.isEmpty()) fields.put("brands", brands);
        if (processorVendors != null && !processorVendors.isEmpty()) fields.put("processorVendors", processorVendors);
        if (processorTiers != null && !processorTiers.isEmpty()) fields.put("processorTiers", processorTiers);
        if (minimumProcessorGeneration != null) fields.put("minimumProcessorGeneration", minimumProcessorGeneration);
        if (minimumRamGb != null) fields.put("minimumRamGb", minimumRamGb);
        if (ramTypes != null && !ramTypes.isEmpty()) fields.put("ramTypes", ramTypes);
        if (minimumStorageGb != null) fields.put("minimumStorageGb", minimumStorageGb);
        if (diskTypes != null && !diskTypes.isEmpty()) fields.put("diskTypes", diskTypes);
        if (screenResolutions != null && !screenResolutions.isEmpty()) fields.put("screenResolutions", screenResolutions);
        if (needsDedicatedGpu != null) fields.put("needsDedicatedGpu", needsDedicatedGpu);
        return fields;
    }
}
