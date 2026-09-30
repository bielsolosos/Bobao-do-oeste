package br.dev.bielsolosos.biscraper.domain.notification.strategy;

import br.dev.bielsolosos.biscraper.core.enums.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NotificationStrategySelector {

    private final Map<NotificationChannel, NotificationStrategy> strategies;

    public NotificationStrategySelector(List<NotificationStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        NotificationStrategy::getChannel,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        () -> new EnumMap<>(NotificationChannel.class)
                ));
        log.info("NotificationStrategySelector inicializado com {} estratégias: {}",
                strategies.size(), strategies.keySet());
    }

    public Optional<NotificationStrategy> getNotificationStrategy(NotificationChannel channel) {
        if (channel == null) {
            return Optional.empty();
        }
        NotificationStrategy strategy = this.strategies.get(channel);
        if (strategy == null) {
            log.warn("Nenhuma estratégia de notificação encontrada para o canal: {}", channel);
            return Optional.empty();
        }
        return Optional.of(strategy);
    }
}
