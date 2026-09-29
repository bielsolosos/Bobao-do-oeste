package br.dev.bielsolosos.biscraper.core.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ModelVendorEnumTest {

    @ParameterizedTest
    @ValueSource(strings = {"GEMINI", "gemini", "Google", "GOOGLE_GENAI", "googlegenai"})
    @DisplayName("Deve mapear variações de nome para GEMINI")
    void shouldMapGeminiAliases(String value) {
        assertEquals(ModelVendorEnum.GEMINI, ModelVendorEnum.fromPropertyValue(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DEEPSEEK", "deepseek", "DeepSeek"})
    @DisplayName("Deve mapear variações de nome para DEEPSEEK")
    void shouldMapDeepSeekAliases(String value) {
        assertEquals(ModelVendorEnum.DEEPSEEK, ModelVendorEnum.fromPropertyValue(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"OLLAMA", "ollama", "Ollama"})
    @DisplayName("Deve mapear variações de nome para OLLAMA")
    void shouldMapOllamaAliases(String value) {
        assertEquals(ModelVendorEnum.OLLAMA, ModelVendorEnum.fromPropertyValue(value));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException para valor inválido ou nulo")
    void shouldThrowExceptionForInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> ModelVendorEnum.fromPropertyValue(null));
        assertThrows(IllegalArgumentException.class, () -> ModelVendorEnum.fromPropertyValue("   "));
        assertThrows(IllegalArgumentException.class, () -> ModelVendorEnum.fromPropertyValue("UNKNOWN_VENDOR"));
    }
}
