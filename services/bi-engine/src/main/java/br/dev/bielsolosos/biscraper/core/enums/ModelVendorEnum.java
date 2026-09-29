package br.dev.bielsolosos.biscraper.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Locale;

@Getter
@AllArgsConstructor
public enum ModelVendorEnum {
    GEMINI("googleGenAiChatModel", "Google Gemini"),
    DEEPSEEK("deepSeekChatModel", "DeepSeek");

    private final String value;
    private final String displayName;

    public static ModelVendorEnum fromPropertyValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Property for model vendor cannot be empty.");
        }

        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "GEMINI", "GOOGLE", "GOOGLE_GENAI", "GOOGLEGENAI" -> GEMINI;
            case "DEEPSEEK" -> DEEPSEEK;
            default -> throw new IllegalArgumentException("Unsupported model vendor: " + value);
        };
    }
}
