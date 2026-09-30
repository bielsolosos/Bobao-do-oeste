package br.dev.bielsolosos.biscraper.domain.monitoring.service;

import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapedListing;
import br.dev.bielsolosos.biscraper.domain.monitoring.notification.MonitoringEmailDigestNotificationTemplate;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ScrapedListingRepository;
import br.dev.bielsolosos.biscraper.domain.notification.event.NotificationEvent;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.model.UserConfig;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserConfigRepository;
import br.dev.bielsolosos.biscraper.infrastructure.BiScraperProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Serviço de domínio responsável por agregar anúncios de alta relevância (HIGH match)
 * e orquestrar o envio de digests periódicos por e-mail para usuários configurados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringEmailDigestService {

    private final UserConfigRepository userConfigRepository;
    private final ScrapedListingRepository scrapedListingRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BiScraperProperties properties;

    /**
     * Executa a rotina de envio de resumos de e-mail para usuários com email_enabled = true.
     */
    @Transactional(readOnly = true)
    public void sendEmailDigests() {
        if (!properties.getEmail().isEnabled()) {
            log.debug("[EmailDigest] Notificações por e-mail desativadas globalmente. Ignorando envio.");
            return;
        }

        int windowHours = properties.getEmail().getDigest().getWindowHours();
        OffsetDateTime since = OffsetDateTime.now().minusHours(windowHours);

        log.info("[EmailDigest] Iniciando ciclo de envio de e-mails periódicos (janela: últimas {} horas, desde {})",
                windowHours, since);

        List<UserConfig> configs = userConfigRepository.findAll();

        for (UserConfig config : configs) {
            if (!config.isEmailEnabled()) {
                continue;
            }

            User user = config.getUser();
            if (user == null || !user.isActive() || user.getEmail() == null || user.getEmail().isBlank()) {
                continue;
            }

            try {
                List<ScrapedListing> highMatchListings = scrapedListingRepository
                        .findByUserIdAndMatchTierAndCreatedAtAfter(user.getId(), MatchTier.HIGH, since);

                if (highMatchListings.isEmpty()) {
                    log.debug("[EmailDigest] Usuário '{}' não possui anúncios HIGH match nas últimas {}h.",
                            user.getUsername(), windowHours);
                    continue;
                }

                String appUrl = properties.getAppUrl();
                int maxItems = properties.getEmail().getDigest().getMaxItems();

                MonitoringEmailDigestNotificationTemplate template =
                        new MonitoringEmailDigestNotificationTemplate(user, highMatchListings, windowHours, appUrl, maxItems);

                NotificationEvent event = NotificationEvent.builder()
                        .recipient(user)
                        .contentTemplate(template)
                        .build();

                eventPublisher.publishEvent(event);

            } catch (Exception ex) {
                log.error("[EmailDigest] Erro ao processar resumo de e-mail para usuário '{}': {}",
                        user.getUsername(), ex.getMessage(), ex);
            }
        }

        log.info("[EmailDigest] Ciclo de e-mails periódicos finalizado.");
    }
}
