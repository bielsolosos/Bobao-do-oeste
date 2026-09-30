package br.dev.bielsolosos.biscraper.domain.notification.strategy;

import br.dev.bielsolosos.biscraper.core.enums.NotificationChannel;
import br.dev.bielsolosos.biscraper.domain.notification.event.NotificationEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NotificationStrategySelectorTest {

    static class DummyDiscordStrategy extends NotificationStrategy {
        public DummyDiscordStrategy() {
            super(null);
        }

        @Override
        public NotificationChannel getChannel() {
            return NotificationChannel.DISCORD;
        }

        @Override
        public void sendNotification(NotificationEvent event) {}
    }

    @Test
    @DisplayName("Deve retornar estratégia correta quando canal estiver registrado")
    void shouldReturnStrategyWhenChannelExists() {
        DummyDiscordStrategy discordStrategy = new DummyDiscordStrategy();
        NotificationStrategySelector selector = new NotificationStrategySelector(List.of(discordStrategy));

        Optional<NotificationStrategy> result = selector.getNotificationStrategy(NotificationChannel.DISCORD);

        assertTrue(result.isPresent());
        assertEquals(NotificationChannel.DISCORD, result.get().getChannel());
    }

    @Test
    @DisplayName("Deve retornar vazio quando canal não estiver registrado ou for nulo")
    void shouldReturnEmptyWhenChannelDoesNotExistOrNull() {
        DummyDiscordStrategy discordStrategy = new DummyDiscordStrategy();
        NotificationStrategySelector selector = new NotificationStrategySelector(List.of(discordStrategy));

        Optional<NotificationStrategy> resultEmail = selector.getNotificationStrategy(NotificationChannel.EMAIL);
        Optional<NotificationStrategy> resultNull = selector.getNotificationStrategy(null);

        assertTrue(resultEmail.isEmpty());
        assertTrue(resultNull.isEmpty());
    }
}
