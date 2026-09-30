package br.dev.bielsolosos.biscraper.domain.notification.service;

import br.dev.bielsolosos.biscraper.core.enums.NotificationChannel;
import br.dev.bielsolosos.biscraper.domain.notification.event.NotificationEvent;
import br.dev.bielsolosos.biscraper.domain.notification.strategy.NotificationStrategy;
import br.dev.bielsolosos.biscraper.domain.notification.strategy.NotificationStrategySelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPublisherService {

    private final NotificationStrategySelector selector;

    @Async("notificationDispatcherExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void receiveEventAndSendNotification(NotificationEvent event) {
        if (event == null || event.getRecipient() == null) {
            log.warn("Evento de notificação ignorado: evento ou destinatário nulo.");
            return;
        }

        NotificationChannel[] channels = (event.getContentTemplate() != null && event.getContentTemplate().getChannels() != null)
                ? event.getContentTemplate().getChannels()
                : new NotificationChannel[]{NotificationChannel.DISCORD};

        String username = event.getRecipient().getUsername();
        log.info("Notificação recebida para usuário '{}'. Canais configurados: {}", username, channels);

        for (NotificationChannel channel : channels) {
            try {
                Optional<NotificationStrategy> strategyOpt = selector.getNotificationStrategy(channel);
                if (strategyOpt.isPresent()) {
                    log.debug("Enviando notificação via canal '{}' para usuário '{}'", channel, username);
                    strategyOpt.get().sendNotification(event);
                } else {
                    log.warn("Canal de notificação '{}' não possui implementação ativa.", channel);
                }
            } catch (Exception ex) {
                log.error("Falha ao enviar notificação via '{}' para usuário '{}': {}",
                        channel, username, ex.getMessage(), ex);
            }
        }
    }
}
