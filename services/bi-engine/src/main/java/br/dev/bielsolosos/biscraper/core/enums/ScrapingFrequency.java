package br.dev.bielsolosos.biscraper.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ScrapingFrequency {
    EVERY_30_MINUTES("0 */30 * * * *", "A cada 30 minutos"),
    HOURLY("0 0 * * * *", "A cada hora"),
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
