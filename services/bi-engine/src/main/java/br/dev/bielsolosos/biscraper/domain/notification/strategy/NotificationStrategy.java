package br.dev.bielsolosos.biscraper.domain.notification.strategy;

import br.dev.bielsolosos.biscraper.core.enums.NotificationChannel;
import br.dev.bielsolosos.biscraper.domain.notification.event.NotificationEvent;
import br.dev.bielsolosos.biscraper.domain.notification.model.NotificationLog;
import br.dev.bielsolosos.biscraper.domain.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class NotificationStrategy {

    protected final NotificationLogRepository repository;

    public abstract NotificationChannel getChannel();

    public abstract void sendNotification(NotificationEvent event);

    public NotificationLog saveLog(NotificationEvent event, String description) {
        if (event.getRecipient() == null) {
            log.warn("Tentativa de salvar log de notificação sem destinatário.");
            return null;
        }

        NotificationLog notificationHistoryEntry = NotificationLog.builder()
                .description(description != null ? description : String.format("Notificação enviada para o usuário '%s' via %s",
                        event.getRecipient().getUsername(), this.getChannel()))
                .notificationChannel(this.getChannel())
                .recipient(event.getRecipient())
                .build();

        return repository.save(notificationHistoryEntry);
    }
}
