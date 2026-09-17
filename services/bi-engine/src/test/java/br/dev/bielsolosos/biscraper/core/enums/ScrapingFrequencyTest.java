package br.dev.bielsolosos.biscraper.core.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.scheduling.support.CronExpression;

import static org.junit.jupiter.api.Assertions.*;

class ScrapingFrequencyTest {

    @ParameterizedTest
    @EnumSource(value = ScrapingFrequency.class, names = {"MANUAL"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Todas as frequências automatizadas devem ter expressões Cron válidas do Spring")
    void shouldHaveValidCronExpressionsForAutomatedFrequencies(ScrapingFrequency frequency) {
        assertNotNull(frequency.getCronExpression(), "CronExpression não deveria ser nula para: " + frequency.name());
        assertNotNull(frequency.getDescription(), "Description não deveria ser nula para: " + frequency.name());
        assertTrue(CronExpression.isValidExpression(frequency.getCronExpression()),
                "Expressão cron inválida para " + frequency.name() + ": " + frequency.getCronExpression());
    }

    @Test
    @DisplayName("MANUAL deve ter cron nulo e descrição válida")
    void manualShouldHaveNullCron() {
        assertNull(ScrapingFrequency.MANUAL.getCronExpression());
        assertEquals("Apenas sob demanda", ScrapingFrequency.MANUAL.getDescription());
    }

    @Test
    @DisplayName("fromCronExpression deve mapear corretamente expressões cron")
    void shouldMapFromCronExpression() {
        assertEquals(ScrapingFrequency.EVERY_MINUTE, ScrapingFrequency.fromCronExpression("0 * * * * *"));
        assertEquals(ScrapingFrequency.EVERY_5_MINUTES, ScrapingFrequency.fromCronExpression("0 */5 * * * *"));
        assertEquals(ScrapingFrequency.EVERY_30_MINUTES, ScrapingFrequency.fromCronExpression("0 */30 * * * *"));
        assertEquals(ScrapingFrequency.HOURLY, ScrapingFrequency.fromCronExpression("0 0 * * * *"));
        assertEquals(ScrapingFrequency.EIGHT_TIMES_DAILY, ScrapingFrequency.fromCronExpression("0 0 */3 * * *"));
        assertEquals(ScrapingFrequency.SIX_TIMES_DAILY, ScrapingFrequency.fromCronExpression("0 0 */4 * * *"));
        assertEquals(ScrapingFrequency.FOUR_TIMES_DAILY, ScrapingFrequency.fromCronExpression("0 0 */6 * * *"));
        assertEquals(ScrapingFrequency.DAILY, ScrapingFrequency.fromCronExpression("0 0 8 * * *"));
        assertEquals(ScrapingFrequency.TWICE_DAILY, ScrapingFrequency.fromCronExpression("0 0 8,18 * * *"));
        assertEquals(ScrapingFrequency.WEEKLY, ScrapingFrequency.fromCronExpression("0 0 8 * * MON"));
    }

    @Test
    @DisplayName("fromCronExpression deve retornar MANUAL para cron nulo, vazio ou desconhecido")
    void shouldReturnManualForUnknownOrEmptyCron() {
        assertEquals(ScrapingFrequency.MANUAL, ScrapingFrequency.fromCronExpression(null));
        assertEquals(ScrapingFrequency.MANUAL, ScrapingFrequency.fromCronExpression(""));
        assertEquals(ScrapingFrequency.MANUAL, ScrapingFrequency.fromCronExpression("   "));
        assertEquals(ScrapingFrequency.MANUAL, ScrapingFrequency.fromCronExpression("0 0 12 * * *"));
    }
}
