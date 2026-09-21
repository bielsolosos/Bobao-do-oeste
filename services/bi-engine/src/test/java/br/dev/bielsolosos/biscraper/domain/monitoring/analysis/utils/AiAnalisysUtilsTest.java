package br.dev.bielsolosos.biscraper.domain.monitoring.analysis.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiAnalisysUtilsTest {

    @Test
    @DisplayName("Deve retornar string vazia quando entrada for nula ou em branco")
    void shouldReturnEmptyWhenInputIsNullOrBlank() {
        assertThat(AiAnalisysUtils.sanitizeJson(null)).isEqualTo("");
        assertThat(AiAnalisysUtils.sanitizeJson("")).isEqualTo("");
        assertThat(AiAnalisysUtils.sanitizeJson("   \n\t  ")).isEqualTo("");
    }

    @Test
    @DisplayName("Deve manter JSON puro inalterado (apenas trim)")
    void shouldKeepPlainJson() {
        String json = "{\"brand\": \"DELL\", \"ramSize\": 16}";
        assertThat(AiAnalisysUtils.sanitizeJson(json)).isEqualTo(json);
        assertThat(AiAnalisysUtils.sanitizeJson("  \n" + json + "\n  ")).isEqualTo(json);
    }

    @Test
    @DisplayName("Deve sanitizar bloco markdown ```json ... ```")
    void shouldSanitizeMarkdownJsonBlock() {
        String json = "[{\"brand\": \"LENOVO\"}]";
        String raw = "```json\n" + json + "\n```";

        assertThat(AiAnalisysUtils.sanitizeJson(raw)).isEqualTo(json);
    }

    @Test
    @DisplayName("Deve sanitizar bloco markdown ```JSON ... ``` em maiúsculo")
    void shouldSanitizeUppercaseMarkdownJsonBlock() {
        String json = "[{\"brand\": \"ACER\"}]";
        String raw = "```JSON\n" + json + "\n```";

        assertThat(AiAnalisysUtils.sanitizeJson(raw)).isEqualTo(json);
    }

    @Test
    @DisplayName("Deve sanitizar bloco markdown genérico ``` ... ```")
    void shouldSanitizeGenericMarkdownBlock() {
        String json = "[{\"brand\": \"APPLE\"}]";
        String raw = "```\n" + json + "\n```";

        assertThat(AiAnalisysUtils.sanitizeJson(raw)).isEqualTo(json);
    }
}
