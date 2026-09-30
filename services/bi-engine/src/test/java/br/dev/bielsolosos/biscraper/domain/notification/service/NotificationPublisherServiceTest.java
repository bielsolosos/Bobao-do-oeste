package br.dev.bielsolosos.biscraper.domain.notification.service;

import br.dev.bielsolosos.biscraper.core.enums.NotificationChannel;
import br.dev.bielsolosos.biscraper.domain.notification.event.NotificationEvent;
import br.dev.bielsolosos.biscraper.domain.notification.strategy.NotificationStrategy;
import br.dev.bielsolosos.biscraper.domain.notification.strategy.NotificationStrategySelector;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPublisherServiceTest {

    @Mock
    private NotificationStrategySelector selector;

    @Mock
    private NotificationStrategy discordStrategy;

    @InjectMocks
    private NotificationPublisherService publisherService;

    @Test
    @DisplayName("Deve despachar notificação para as estratégias correspondentes")
    void shouldDispatchNotificationToRegisteredStrategy() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("bielsolosos")
                .email("biel@dev.com")
                .build();

        NotificationEvent event = NotificationEvent.builder()
                .recipient(user)
                .contentTemplate(NotificationEvent.GenericNotificationTemplate.builder()
                        .channels(new NotificationChannel[]{NotificationChannel.DISCORD})
                        .message("Teste de alerta")
                        .build())
                .build();

        when(selector.getNotificationStrategy(NotificationChannel.DISCORD)).thenReturn(Optional.of(discordStrategy));

        publisherService.receiveEventAndSendNotification(event);

        verify(discordStrategy, times(1)).sendNotification(event);
    }

    @Test
    @DisplayName("Deve ignorar silenciosamente quando evento ou destinatário for nulo")
    void shouldIgnoreNullEventOrRecipient() {
        publisherService.receiveEventAndSendNotification(null);

        NotificationEvent eventWithoutRecipient = NotificationEvent.builder().build();
        publisherService.receiveEventAndSendNotification(eventWithoutRecipient);

        verifyNoInteractions(selector);
    }
}
