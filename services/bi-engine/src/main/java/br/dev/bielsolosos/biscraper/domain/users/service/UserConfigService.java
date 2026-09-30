package br.dev.bielsolosos.biscraper.domain.users.service;

import br.dev.bielsolosos.biscraper.core.enums.LlmModelEnum;
import br.dev.bielsolosos.biscraper.core.enums.ModelVendorEnum;
import br.dev.bielsolosos.biscraper.domain.users.mapper.UserConfigMapper;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.model.UserConfig;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.AvailableAiModelsResponse;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.UserConfigRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.UserConfigResponse;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserConfigService {

    private final UserConfigRepository userConfigRepository;

    @Transactional
    public UserConfig getConfigForUser(User user) {
        if (user == null || user.getId() == null) {
            return getDefaultConfig(user);
        }

        return userConfigRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    log.info("Criando configuração padrão de IA para o usuário '{}'", user.getUsername());
                    UserConfig defaultConfig = UserConfig.builder()
                            .user(user)
                            .aiVendor(ModelVendorEnum.GEMINI)
                            .cheapModel(LlmModelEnum.GEMINI_2_5_FLASH.getModel())
                            .strongModel(LlmModelEnum.GEMINI_2_5_PRO.getModel())
                            .build();
                    return userConfigRepository.save(defaultConfig);
                });
    }

    @Transactional
    public UserConfigResponse updateConfigForUser(User user, UserConfigRequest request) {
        log.info("Atualizando configurações de IA para o usuário '{}': vendor='{}', cheap='{}', strong='{}'",
                user.getUsername(), request.aiVendor(), request.cheapModel(), request.strongModel());

        UserConfig config = getConfigForUser(user);
        config.setAiVendor(request.aiVendor());
        config.setCheapModel(request.cheapModel().trim());
        config.setStrongModel(request.strongModel().trim());

        if (request.discordWebhookUrl() != null) {
            config.setDiscordWebhookUrl(request.discordWebhookUrl().trim());
        }
        if (request.discordEnabled() != null) {
            config.setDiscordEnabled(request.discordEnabled());
        }
        if (request.emailEnabled() != null) {
            config.setEmailEnabled(request.emailEnabled());
        }

        UserConfig saved = userConfigRepository.save(config);
        return UserConfigMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AvailableAiModelsResponse getAvailableModelsCatalog() {
        List<AvailableAiModelsResponse.VendorModelsDto> vendorList = new ArrayList<>();

        for (ModelVendorEnum vendor : List.of(ModelVendorEnum.GEMINI, ModelVendorEnum.DEEPSEEK)) {
            List<AvailableAiModelsResponse.ModelOptionDto> cheapModels = LlmModelEnum.getByVendorAndTier(vendor, LlmModelEnum.ModelTier.CHEAP)
                    .stream()
                    .map(m -> new AvailableAiModelsResponse.ModelOptionDto(m.getModel(), m.getDescription(), m.getDescription()))
                    .toList();

            List<AvailableAiModelsResponse.ModelOptionDto> strongModels = LlmModelEnum.getByVendorAndTier(vendor, LlmModelEnum.ModelTier.STRONG)
                    .stream()
                    .map(m -> new AvailableAiModelsResponse.ModelOptionDto(m.getModel(), m.getDescription(), m.getDescription()))
                    .toList();

            vendorList.add(new AvailableAiModelsResponse.VendorModelsDto(
                    vendor,
                    vendor.getDisplayName(),
                    cheapModels,
                    strongModels
            ));
        }

        return new AvailableAiModelsResponse(vendorList);
    }

    private UserConfig getDefaultConfig(User user) {
        return UserConfig.builder()
                .user(user)
                .aiVendor(ModelVendorEnum.GEMINI)
                .cheapModel(LlmModelEnum.GEMINI_2_5_FLASH.getModel())
                .strongModel(LlmModelEnum.GEMINI_2_5_PRO.getModel())
                .build();
    }
}
