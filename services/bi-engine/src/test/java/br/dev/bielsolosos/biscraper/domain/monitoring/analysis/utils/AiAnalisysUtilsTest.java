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

    @Test
    @DisplayName("Deve extrair JSON quando o LLM retornar texto conversacional antes e depois")
    void shouldExtractJsonWithConversationalText() {
        String json = "[{\"brand\": \"DELL\", \"ramSize\": 16}]";
        String raw = "Tool getAdditionalInfo was executed successfully.\nHere is the analysis:\n```json\n" + json + "\n```\nHope this helps!";

        assertThat(AiAnalisysUtils.sanitizeJson(raw)).isEqualTo(json);
    }

    @Test
    @DisplayName("Deve extrair JSON puro mesmo com prefixo de Tool sem markdown")
    void shouldExtractJsonWithToolPrefixWithoutMarkdown() {
        String json = "[{\"brand\": \"DELL\"}]";
        String raw = "Tool execution completed. Results:\n" + json;

        assertThat(AiAnalisysUtils.sanitizeJson(raw)).isEqualTo(json);
    }

    @Test
    @DisplayName("Deve extrair objeto JSON com texto conversacional")
    void shouldExtractJsonObjectWithConversationalText() {
        String json = "{\"brand\": \"DELL\", \"score\": 90}";
        String raw = "I have evaluated the item:\n" + json + "\nThank you.";

        assertThat(AiAnalisysUtils.sanitizeJson(raw)).isEqualTo(json);
    }
}
