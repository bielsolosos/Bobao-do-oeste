package br.dev.bielsolosos.biscraper.domain.users.model.dto;

import br.dev.bielsolosos.biscraper.core.enums.ModelVendorEnum;

import java.util.List;

public record AvailableAiModelsResponse(
        List<VendorModelsDto> vendors
) {
    public record VendorModelsDto(
            ModelVendorEnum vendor,
            String displayName,
            List<ModelOptionDto> cheapModels,
            List<ModelOptionDto> strongModels
    ) {}

    public record ModelOptionDto(
            String id,
            String name,
            String description
    ) {}
}
