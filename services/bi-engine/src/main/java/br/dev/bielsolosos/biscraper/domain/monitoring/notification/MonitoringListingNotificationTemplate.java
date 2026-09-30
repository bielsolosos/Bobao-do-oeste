package br.dev.bielsolosos.biscraper.domain.monitoring.notification;

import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.core.enums.NotificationChannel;
import br.dev.bielsolosos.biscraper.core.utils.NotificationTemplate;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapedListing;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.util.*;

@Getter
public class MonitoringListingNotificationTemplate implements NotificationTemplate {

    private final ProductMonitor monitor;
    private final List<ScrapedListing> listings;
    private final NotificationChannel[] channels;
    private static final Locale PT_BR = Locale.of("pt", "BR");

    public MonitoringListingNotificationTemplate(ProductMonitor monitor, List<ScrapedListing> listings) {
        this(monitor, listings, new NotificationChannel[]{NotificationChannel.DISCORD});
    }

    public MonitoringListingNotificationTemplate(ProductMonitor monitor, List<ScrapedListing> listings, NotificationChannel[] channels) {
        this.monitor = monitor;
        this.listings = listings != null ? listings : Collections.emptyList();
        this.channels = (channels != null && ArrayUtils.isNotEmpty(channels))
                ? channels
                : new NotificationChannel[]{NotificationChannel.DISCORD};
    }

    @Override
    public NotificationChannel[] getChannels() {
        return this.channels;
    }

    @Override
    public String getSubject() {
        String monitorName = monitor != null ? monitor.getName() : "Monitoramento";
        return String.format("🎯 Novos anúncios encontrados para %s (%d novos)", monitorName, listings.size());
    }

    @Override
    public String getMessageTemplate() {
        String monitorName = monitor != null ? monitor.getName() : "Monitoramento";
        if (listings.isEmpty()) {
            return String.format("Nenhum anúncio novo encontrado para o monitor '%s'.", monitorName);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🎯 **Novos anúncios encontrados para o monitor `%s`:**\n\n", monitorName));

        for (ScrapedListing listing : listings) {
            String price = listing.getCurrentPrice() != null
                    ? NumberFormat.getCurrencyInstance(PT_BR).format(listing.getCurrentPrice())
                    : "N/A";
            String tier = listing.getMatchTier() != null ? listing.getMatchTier().name() : "N/A";
            String score = listing.getMatchScore() != null ? listing.getMatchScore().toPlainString() + "%" : "0%";

            sb.append(String.format("• **%s**\n  - Preço: %s\n  - Match: %s (%s)\n  - Link: %s\n\n",
                    listing.getTitle(), price, tier, score, listing.getUrl()));
        }

        return sb.toString();
    }

    @Override
    public Map<String, Object> toDiscordPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "BI Scraper");
        payload.put("avatar_url", "https://raw.githubusercontent.com/bielsolosos/projeto-scrap/main/assets/bot-avatar.png");
        payload.put("content", getSubject());

        List<Map<String, Object>> embeds = buildMonitoringEmbeds();
        if (!embeds.isEmpty()) {
            payload.put("embeds", embeds);
        }

        return payload;
    }

    private List<Map<String, Object>> buildMonitoringEmbeds() {
        if (listings.isEmpty()) {
            return Collections.emptyList();
        }

        String monitorName = monitor != null ? monitor.getName() : "Monitoramento";
        List<Map<String, Object>> embeds = new ArrayList<>();

        // Discord permite no máximo 10 embeds por mensagem
        int count = Math.min(listings.size(), 10);
        for (int i = 0; i < count; i++) {
            ScrapedListing listing = listings.get(i);
            embeds.add(createListingEmbed(monitorName, listing));
        }

        return embeds;
    }

    private Map<String, Object> createListingEmbed(String monitorName, ScrapedListing listing) {
        Map<String, Object> embed = new HashMap<>();
        embed.put("title", StringUtils.abbreviate(listing.getTitle(), 250));
        embed.put("url", listing.getUrl());

        int color = 0x5865F2; // Padrão Blurple
        if (listing.getMatchTier() == MatchTier.HIGH) {
            color = 0x57F287; // Verde
        } else if (listing.getMatchTier() == MatchTier.MEDIUM) {
            color = 0xFEE75C; // Amarelo
        } else if (listing.getMatchTier() == MatchTier.LOW) {
            color = 0xED4245; // Vermelho
        }
        embed.put("color", color);

        if (StringUtils.isNotBlank(listing.getDescription())) {
            embed.put("description", StringUtils.abbreviate(listing.getDescription(), 200));
        }

        List<Map<String, Object>> fields = new ArrayList<>();

        String priceFormatted = listing.getCurrentPrice() != null
                ? NumberFormat.getCurrencyInstance(PT_BR).format(listing.getCurrentPrice())
                : "N/A";
        fields.add(Map.of("name", "💰 Preço", "value", priceFormatted, "inline", true));

        fields.add(Map.of("name", "🏪 Plataforma", "value", listing.getVendor() != null ? listing.getVendor().name() : "N/A", "inline", true));

        String matchValue = String.format("%s (%s%%)",
                listing.getMatchTier() != null ? listing.getMatchTier().name() : "N/A",
                listing.getMatchScore() != null ? listing.getMatchScore().toPlainString() : "0");
        fields.add(Map.of("name", "🎯 Match", "value", matchValue, "inline", true));

        String local = formatLocation(listing);
        if (StringUtils.isNotBlank(local)) {
            fields.add(Map.of("name", "📍 Local", "value", local, "inline", true));
        }

        if (listing.isHasDelivery()) {
            fields.add(Map.of("name", "🚚 Entrega", "value", listing.getDeliveryType() != null ? listing.getDeliveryType() : "Disponível", "inline", true));
        }

        embed.put("fields", fields);

        if (listing.getImages() != null && !listing.getImages().isEmpty()) {
            String firstImage = listing.getImages().get(0);
            if (StringUtils.isNotBlank(firstImage) && firstImage.startsWith("http")) {
                embed.put("thumbnail", Map.of("url", firstImage));
            }
        }

        embed.put("footer", Map.of("text", "BI Scraper • " + monitorName));
        embed.put("timestamp", OffsetDateTime.now().toString());

        return embed;
    }

    private String formatLocation(ScrapedListing listing) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotBlank(listing.getNeighborhood())) parts.add(listing.getNeighborhood());
        if (StringUtils.isNotBlank(listing.getCity())) parts.add(listing.getCity());
        if (StringUtils.isNotBlank(listing.getState())) parts.add(listing.getState());
        return String.join(", ", parts);
    }
}
