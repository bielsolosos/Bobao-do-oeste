package br.dev.bielsolosos.biscraper.domain.notification.strategy.impl;

import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.core.enums.NotificationChannel;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapedListing;
import br.dev.bielsolosos.biscraper.domain.monitoring.notification.MonitoringListingNotificationTemplate;
import br.dev.bielsolosos.biscraper.domain.notification.event.NotificationEvent;
import br.dev.bielsolosos.biscraper.domain.notification.model.NotificationLog;
import br.dev.bielsolosos.biscraper.domain.notification.repository.NotificationLogRepository;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.model.UserConfig;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserConfigRepository;
import br.dev.bielsolosos.biscraper.infrastructure.client.discord.DiscordWebhookHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDiscordStrategyTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private UserConfigRepository userConfigRepository;

    @Mock
    private DiscordWebhookHttpClient discordWebhookHttpClient;

    private NotificationDiscordStrategy discordStrategy;

    @BeforeEach
    void setUp() {
        discordStrategy = new NotificationDiscordStrategy(
                notificationLogRepository,
                userConfigRepository,
                discordWebhookHttpClient
        );
    }

    @Test
    @DisplayName("Deve retornar DISCORD como canal suportado")
    void shouldReturnDiscordChannel() {
        assertEquals(NotificationChannel.DISCORD, discordStrategy.getChannel());
    }

    @Test
    @DisplayName("Deve enviar webhook do Discord e salvar log quando configurado com sucesso")
    void shouldSendDiscordNotificationSuccessfully() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("bielsolosos")
                .email("biel@dev.com")
                .build();

        UserConfig config = UserConfig.builder()
                .id(UUID.randomUUID())
                .user(user)
                .discordEnabled(true)
                .discordWebhookUrl("https://discord.com/api/webhooks/123/token")
                .build();

        ProductMonitor monitor = ProductMonitor.builder()
                .id(UUID.randomUUID())
                .name("Macbook M2")
                .build();

        ScrapedListing listing = ScrapedListing.builder()
                .id(UUID.randomUUID())
                .title("Macbook Air M2 8GB 256GB")
                .vendor(Vendor.OLX)
                .url("https://olx.com.br/item123")
                .currentPrice(BigDecimal.valueOf(4200))
                .matchTier(MatchTier.HIGH)
                .matchScore(BigDecimal.valueOf(95.5))
                .city("São Paulo")
                .state("SP")
                .hasDelivery(true)
                .deliveryType("OLX_PAY")
                .images(List.of("https://img.olx.com.br/item.jpg"))
                .build();

        MonitoringListingNotificationTemplate template = new MonitoringListingNotificationTemplate(
                monitor,
                List.of(listing)
        );

        NotificationEvent event = NotificationEvent.builder()
                .recipient(user)
                .contentTemplate(template)
                .build();

        when(userConfigRepository.findByUserId(userId)).thenReturn(Optional.of(config));

        discordStrategy.sendNotification(event);

        verify(discordWebhookHttpClient, times(1)).sendWebhook(eq("https://discord.com/api/webhooks/123/token"), any());
        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, times(1)).save(logCaptor.capture());

        NotificationLog savedLog = logCaptor.getValue();
        assertNotNull(savedLog);
        assertEquals(user, savedLog.getRecipient());
        assertEquals(NotificationChannel.DISCORD, savedLog.getNotificationChannel());
    }

    @Test
    @DisplayName("Não deve disparar chamada HTTP se o usuário não tiver Webhook configurado")
    void shouldNotSendIfDiscordWebhookNotConfigured() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("bielsolosos").build();
        UserConfig config = UserConfig.builder().user(user).discordEnabled(true).discordWebhookUrl(null).build();

        NotificationEvent event = NotificationEvent.builder().recipient(user).build();

        when(userConfigRepository.findByUserId(userId)).thenReturn(Optional.of(config));

        discordStrategy.sendNotification(event);

        verifyNoInteractions(discordWebhookHttpClient);
        verifyNoInteractions(notificationLogRepository);
    }
}
