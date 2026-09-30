package br.dev.bielsolosos.biscraper.domain.notification.strategy.impl;

import br.dev.bielsolosos.biscraper.core.enums.NotificationChannel;
import br.dev.bielsolosos.biscraper.core.utils.NotificationTemplate;
import br.dev.bielsolosos.biscraper.domain.notification.event.NotificationEvent;
import br.dev.bielsolosos.biscraper.domain.notification.repository.NotificationLogRepository;
import br.dev.bielsolosos.biscraper.domain.notification.strategy.NotificationStrategy;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.model.UserConfig;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserConfigRepository;
import br.dev.bielsolosos.biscraper.infrastructure.client.discord.DiscordWebhookHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class NotificationDiscordStrategy extends NotificationStrategy {

    private final UserConfigRepository userConfigRepository;
    private final DiscordWebhookHttpClient discordWebhookHttpClient;

    public NotificationDiscordStrategy(
            NotificationLogRepository notificationLogRepository,
            UserConfigRepository userConfigRepository,
            DiscordWebhookHttpClient discordWebhookHttpClient
    ) {
        super(notificationLogRepository);
        this.userConfigRepository = userConfigRepository;
        this.discordWebhookHttpClient = discordWebhookHttpClient;
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.DISCORD;
    }

    @Override
    public void sendNotification(NotificationEvent event) {
        User recipient = event.getRecipient();
        if (recipient == null) {
            log.warn("Tentativa de enviar notificação Discord sem usuário destinatário.");
            return;
        }

        Optional<UserConfig> configOpt = userConfigRepository.findByUserId(recipient.getId());
        if (configOpt.isEmpty() || !configOpt.get().isDiscordEnabled()) {
            log.info("Notificações do Discord estão desativadas para o usuário '{}'", recipient.getUsername());
            return;
        }

        String webhookUrl = configOpt.get().getDiscordWebhookUrl();
        if (StringUtils.isBlank(webhookUrl)) {
            log.info("Usuário '{}' não configurou URL de Webhook do Discord.", recipient.getUsername());
            return;
        }

        try {
            NotificationTemplate template = event.getContentTemplate();
            Map<String, Object> discordPayload = template != null
                    ? template.toDiscordPayload()
                    : Map.of("content", "🔔 Notificação do BI Scraper");

            log.info("Disparando webhook do Discord para usuário '{}' em {}", recipient.getUsername(), maskWebhookUrl(webhookUrl));

            discordWebhookHttpClient.sendWebhook(webhookUrl, discordPayload);

            String logDescription = String.format(
                    "Notificação Discord enviada para '%s' com assunto: '%s'.",
                    recipient.getUsername(),
                    template != null ? template.getSubject() : "N/A"
            );
            saveLog(event, logDescription);
            log.info("Notificação Discord entregue com sucesso para '{}'", recipient.getUsername());

        } catch (Exception e) {
            log.error("Erro ao enviar webhook do Discord para usuário '{}': {}", recipient.getUsername(), e.getMessage(), e);
            saveLog(event, "Falha ao enviar webhook do Discord: " + e.getMessage());
        }
    }

    private String maskWebhookUrl(String url) {
        if (url == null || url.length() < 20) return "***";
        return url.substring(0, 35) + "...";
    }
}
