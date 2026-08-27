package br.dev.bielsolosos.biscraper.api.model.productmonitor;

import br.dev.bielsolosos.biscraper.core.abstractfields.AnalysisTypeFields;
import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.core.enums.ScrapingFrequency;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProductMonitorRequest(
    @NotBlank(message = "Nome do monitor é obrigatório.")
    @Size(max = 150, message = "Nome do monitor não pode exceder 150 caracteres.")
    String name,

    String description,

    @NotNull(message = "Vendor alvo (ex: OLX, MERCADO_LIVRE) é obrigatório.")
    Vendor vendor,

    @NotNull(message = "Tipo de análise (ex: SIMPLE, NOTEBOOK) é obrigatório.")
    AnalysisType analysisType,

    @Valid
    AnalysisTypeFields analysisTypeFields,

    @NotEmpty(message = "Ao menos uma palavra-chave para busca deve ser informada.")
    List<@NotBlank(message = "Palavra-chave não pode ser vazia.") String> searchKeywords,

    @PositiveOrZero(message = "Preço mínimo não pode ser negativo.")
    BigDecimal minPrice,

    @PositiveOrZero(message = "Preço máximo não pode ser negativo.")
    BigDecimal maxPrice,

    String stateFilter,
    String regionFilter,
    Boolean requireDelivery,
    ScrapingFrequency frequency
) {}
