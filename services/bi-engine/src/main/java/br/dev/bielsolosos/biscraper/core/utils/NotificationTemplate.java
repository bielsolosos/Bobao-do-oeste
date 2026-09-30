package br.dev.bielsolosos.biscraper.core.utils;

import br.dev.bielsolosos.biscraper.core.enums.NotificationChannel;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface NotificationTemplate {

    NotificationChannel[] getChannels();

    String getSubject();

    String getMessageTemplate();

    default Map<String, Object> toDiscordPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", "BI Scraper");
        payload.put("avatar_url", "https://raw.githubusercontent.com/bielsolosos/projeto-scrap/main/assets/bot-avatar.png");
        payload.put("content", getSubject());

        Map<String, Object> genericEmbed = new HashMap<>();
        genericEmbed.put("title", getSubject());
        genericEmbed.put("description", getMessageTemplate());
        genericEmbed.put("color", 0x5865F2); // Blurple
        genericEmbed.put("timestamp", OffsetDateTime.now().toString());

        payload.put("embeds", List.of(genericEmbed));
        return payload;
    }

    default String toHtmlEmail() {
        String msg = getMessageTemplate() != null ? getMessageTemplate().replace("\n", "<br/>") : "";
        return "<!DOCTYPE html><html lang=\"pt-BR\"><head><meta charset=\"UTF-8\"></head><body style=\"font-family: sans-serif; padding: 20px;\"><h2>"
                + getSubject() + "</h2><p>" + msg + "</p></body></html>";
    }
}
