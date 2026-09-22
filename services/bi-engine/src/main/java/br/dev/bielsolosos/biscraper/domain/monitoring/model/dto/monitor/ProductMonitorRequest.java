package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.monitor;

import br.dev.bielsolosos.biscraper.core.abstractfields.AnalysisTypeFields;
import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.core.enums.ScrapingFrequency;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductMonitorRequest {

    @NotBlank(message = "Nome do monitor é obrigatório.")
    @Size(max = 150, message = "Nome do monitor não pode exceder 150 caracteres.")
    private String name;

    private String description;

    @NotNull(message = "Vendor alvo (ex: OLX, MERCADO_LIVRE) é obrigatório.")
    private Vendor vendor;

    @NotNull(message = "Tipo de análise (ex: SIMPLE, NOTEBOOK) é obrigatório.")
    private AnalysisType analysisType;

    @Valid
    private AnalysisTypeFields analysisTypeFields;

    @NotEmpty(message = "Ao menos uma palavra-chave para busca deve ser informada.")
    private List<@NotBlank(message = "Palavra-chave não pode ser vazia.") String> searchKeywords;

    @PositiveOrZero(message = "Preço mínimo não pode ser negativo.")
    private BigDecimal minPrice;

    @PositiveOrZero(message = "Preço máximo não pode ser negativo.")
    private BigDecimal maxPrice;

    private String stateFilter;
    private String regionFilter;

    @JsonAlias({"requiredDelivery", "require_delivery", "required_delivery"})
    private Boolean requireDelivery;

    private ScrapingFrequency frequency;

    // Métodos de conveniência no estilo record
    public String name() { return name; }
    public String description() { return description; }
    public Vendor vendor() { return vendor; }
    public AnalysisType analysisType() { return analysisType; }
    public AnalysisTypeFields analysisTypeFields() { return analysisTypeFields; }
    public List<String> searchKeywords() { return searchKeywords; }
    public BigDecimal minPrice() { return minPrice; }
    public BigDecimal maxPrice() { return maxPrice; }
    public String stateFilter() { return stateFilter; }
    public String regionFilter() { return regionFilter; }
    public Boolean requireDelivery() { return requireDelivery; }
    public Boolean requiredDelivery() { return requireDelivery; }
    public ScrapingFrequency frequency() { return frequency; }
}
