package br.dev.bielsolosos.biscraper.domain.users.model.dto;

import br.dev.bielsolosos.biscraper.core.enums.ModelVendorEnum;

import java.util.UUID;

public record UserConfigResponse(
        UUID id,
        ModelVendorEnum aiVendor,
        String cheapModel,
        String strongModel,
        String discordWebhookUrl,
        boolean discordEnabled,
        boolean emailEnabled
) {
    public UserConfigResponse(UUID id, ModelVendorEnum aiVendor, String cheapModel, String strongModel) {
        this(id, aiVendor, cheapModel, strongModel, null, true, false);
    }
}
