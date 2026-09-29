package br.dev.bielsolosos.biscraper.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum LlmModelEnum {

    // Gemini Models
    GEMINI_2_5_FLASH("gemini-2.5-flash", "Gemini 2.5 Flash", ModelVendorEnum.GEMINI, ModelTier.CHEAP),
    GEMINI_2_5_FLASH_LITE("gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite", ModelVendorEnum.GEMINI, ModelTier.CHEAP),
    GEMINI_2_5_PRO("gemini-2.5-pro", "Gemini 2.5 Pro", ModelVendorEnum.GEMINI, ModelTier.STRONG),
    GEMINI_1_5_FLASH("gemini-1.5-flash", "Gemini 1.5 Flash", ModelVendorEnum.GEMINI, ModelTier.CHEAP),
    GEMINI_1_5_PRO("gemini-1.5-pro", "Gemini 1.5 Pro", ModelVendorEnum.GEMINI, ModelTier.STRONG),

    // DeepSeek Models
    DEEPSEEK_CHAT("deepseek-chat", "DeepSeek Chat (V3)", ModelVendorEnum.DEEPSEEK, ModelTier.CHEAP),
    DEEPSEEK_V4_1_FLASH("deepseek-v4.1-flash", "DeepSeek V4.1 FLASH", ModelVendorEnum.DEEPSEEK, ModelTier.CHEAP),
    DEEPSEEK_V4_FLASH("deepseek-v4-flash", "DeepSeek V4 FLASH", ModelVendorEnum.DEEPSEEK, ModelTier.CHEAP),
    DEEPSEEK_REASONER("deepseek-reasoner", "DeepSeek Reasoner (R1)", ModelVendorEnum.DEEPSEEK, ModelTier.STRONG),
    DEEPSEEK_V4_PRO("deepseek-v4-pro-0813", "DeepSeek Reasoner (R1)", ModelVendorEnum.DEEPSEEK, ModelTier.STRONG);

    private final String model;
    private final String description;
    private final ModelVendorEnum vendor;
    private final ModelTier tier;

    public enum ModelTier {
        CHEAP,
        STRONG
    }

    public static List<LlmModelEnum> getByVendor(ModelVendorEnum vendor) {
        return Arrays.stream(values())
                .filter(m -> m.getVendor() == vendor)
                .toList();
    }

    public static List<LlmModelEnum> getByVendorAndTier(ModelVendorEnum vendor, ModelTier tier) {
        return Arrays.stream(values())
                .filter(m -> m.getVendor() == vendor && m.getTier() == tier)
                .toList();
    }

    public static LlmModelEnum fromModelName(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(m -> m.getModel().equalsIgnoreCase(modelName.trim()))
                .findFirst()
                .orElse(null);
    }
}
