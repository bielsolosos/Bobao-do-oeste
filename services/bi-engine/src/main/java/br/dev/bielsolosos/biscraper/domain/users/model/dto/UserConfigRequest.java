package br.dev.bielsolosos.biscraper.domain.users.model.dto;

import br.dev.bielsolosos.biscraper.core.enums.ModelVendorEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserConfigRequest(
        @NotNull(message = "O provedor de IA (aiVendor) é obrigatório.")
        ModelVendorEnum aiVendor,

        @NotBlank(message = "O modelo rápido (cheapModel) é obrigatório.")
        String cheapModel,

        @NotBlank(message = "O modelo avançado (strongModel) é obrigatório.")
        String strongModel,

        String discordWebhookUrl,

        Boolean discordEnabled,

        Boolean emailEnabled
) {
    public UserConfigRequest(ModelVendorEnum aiVendor, String cheapModel, String strongModel) {
        this(aiVendor, cheapModel, strongModel, null, true, false);
    }
}
