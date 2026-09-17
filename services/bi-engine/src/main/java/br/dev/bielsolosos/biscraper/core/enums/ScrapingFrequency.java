package br.dev.bielsolosos.biscraper.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScrapingFrequency {
    EVERY_MINUTE("0 * * * * *", "A cada minuto"),
    EVERY_5_MINUTES("0 */5 * * * *", "A cada 5 minutos"),
    EVERY_30_MINUTES("0 */30 * * * *", "A cada 30 minutos"),
    HOURLY("0 0 * * * *", "A cada hora"),
    EIGHT_TIMES_DAILY("0 0 */3 * * *", "8 vezes ao dia (a cada 3 horas)"),
    SIX_TIMES_DAILY("0 0 */4 * * *", "6 vezes ao dia (a cada 4 horas)"),
    FOUR_TIMES_DAILY("0 0 */6 * * *", "4 vezes ao dia (a cada 6 horas)"),
    EVERY_6_HOURS("0 0 */6 * * *", "A cada 6 horas"),
    DAILY("0 0 8 * * *", "Diariamente"),
    TWICE_DAILY("0 0 8,18 * * *", "Duas vezes ao dia"),
    WEEKLY("0 0 8 * * MON", "Semanalmente"),
    MANUAL(null, "Apenas sob demanda");

    private final String cronExpression;
    private final String description;

    public static ScrapingFrequency fromCronExpression(String cron) {
        if (cron == null || cron.isBlank()) {
            return MANUAL;
        }
        for (ScrapingFrequency freq : values()) {
            if (freq.getCronExpression() != null && freq.getCronExpression().equalsIgnoreCase(cron)) {
                return freq;
            }
        }
        return MANUAL;
    }
}
