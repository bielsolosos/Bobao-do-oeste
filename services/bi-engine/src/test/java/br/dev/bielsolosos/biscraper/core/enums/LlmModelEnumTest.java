package br.dev.bielsolosos.biscraper.core.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LlmModelEnumTest {

    @Test
    @DisplayName("Deve filtrar modelos corretamente por Vendor")
    void shouldFilterByVendor() {
        List<LlmModelEnum> geminiModels = LlmModelEnum.getByVendor(ModelVendorEnum.GEMINI);
        assertFalse(geminiModels.isEmpty());
        assertTrue(geminiModels.stream().allMatch(m -> m.getVendor() == ModelVendorEnum.GEMINI));

        List<LlmModelEnum> deepSeekModels = LlmModelEnum.getByVendor(ModelVendorEnum.DEEPSEEK);
        assertFalse(deepSeekModels.isEmpty());
        assertTrue(deepSeekModels.stream().allMatch(m -> m.getVendor() == ModelVendorEnum.DEEPSEEK));
    }

    @Test
    @DisplayName("Deve filtrar modelos por Vendor e Tier")
    void shouldFilterByVendorAndTier() {
        List<LlmModelEnum> geminiCheap = LlmModelEnum.getByVendorAndTier(ModelVendorEnum.GEMINI, LlmModelEnum.ModelTier.CHEAP);
        assertFalse(geminiCheap.isEmpty());
        assertTrue(geminiCheap.contains(LlmModelEnum.GEMINI_2_5_FLASH));

        List<LlmModelEnum> geminiStrong = LlmModelEnum.getByVendorAndTier(ModelVendorEnum.GEMINI, LlmModelEnum.ModelTier.STRONG);
        assertFalse(geminiStrong.isEmpty());
        assertTrue(geminiStrong.contains(LlmModelEnum.GEMINI_2_5_PRO));

        List<LlmModelEnum> deepSeekCheap = LlmModelEnum.getByVendorAndTier(ModelVendorEnum.DEEPSEEK, LlmModelEnum.ModelTier.CHEAP);
        assertTrue(deepSeekCheap.contains(LlmModelEnum.DEEPSEEK_FLASH));

        List<LlmModelEnum> deepSeekStrong = LlmModelEnum.getByVendorAndTier(ModelVendorEnum.DEEPSEEK, LlmModelEnum.ModelTier.STRONG);
        assertTrue(deepSeekStrong.contains(LlmModelEnum.DEEPSEEK_V4_PRO));
    }

    @Test
    @DisplayName("Deve resolver modelo a partir da string com fromModelName")
    void shouldResolveFromModelName() {
        assertEquals(LlmModelEnum.DEEPSEEK_FLASH, LlmModelEnum.fromModelName("deepseek-flash"));
        assertEquals(LlmModelEnum.GEMINI_2_5_FLASH, LlmModelEnum.fromModelName("gemini-2.5-flash"));
        assertNull(LlmModelEnum.fromModelName("inexistente"));
        assertNull(LlmModelEnum.fromModelName(null));
    }
}
