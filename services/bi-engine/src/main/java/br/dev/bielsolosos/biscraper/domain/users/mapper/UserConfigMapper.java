package br.dev.bielsolosos.biscraper.domain.users.mapper;

import br.dev.bielsolosos.biscraper.domain.users.model.UserConfig;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.UserConfigResponse;

public class UserConfigMapper {

    private UserConfigMapper() {}

    public static UserConfigResponse toResponse(UserConfig config) {
        if (config == null) {
            return null;
        }
        return new UserConfigResponse(
                config.getId(),
                config.getAiVendor(),
                config.getCheapModel(),
                config.getStrongModel(),
                config.getDiscordWebhookUrl(),
                config.isDiscordEnabled(),
                config.isEmailEnabled()
        );
    }
}
