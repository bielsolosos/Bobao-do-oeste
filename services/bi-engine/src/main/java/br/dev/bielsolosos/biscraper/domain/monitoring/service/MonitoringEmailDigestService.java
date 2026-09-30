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
            log.debug("[EmailDigest] Notificações por e-mail desativadas globalmente (biscraper.email.enabled = false). Ignorando envio.");
            return;
        }

        int windowHours = properties.getEmail().getDigest().getWindowHours();
        OffsetDateTime since = OffsetDateTime.now().minusHours(windowHours);

        log.info("[EmailDigest] Iniciando ciclo de envio de digests por e-mail (janela: últimas {} horas, corte: {}).",
                windowHours, since);

        List<UserConfig> configs = userConfigRepository.findAll();
        log.info("[EmailDigest] Total de configurações de usuários recuperadas para análise: {}", configs.size());

        int digestsDispatched = 0;
        int skippedDisabledOrInvalid = 0;
        int skippedNoListings = 0;

        for (UserConfig config : configs) {
            if (!config.isEmailEnabled()) {
                String username = config.getUser() != null ? config.getUser().getUsername() : "desconhecido";
                log.debug("[EmailDigest] Usuário '{}' possui notificações por e-mail desativadas (email_enabled = false). Pulando.", username);
                skippedDisabledOrInvalid++;
                continue;
            }

            User user = config.getUser();
            if (user == null || !user.isActive() || user.getEmail() == null || user.getEmail().isBlank()) {
                String identifier = user != null ? user.getUsername() : "ID desconhecido";
                log.warn("[EmailDigest] Usuário '{}' está inativo, sem e-mail válido ou nulo. Ignorando envio de digest.", identifier);
                skippedDisabledOrInvalid++;
                continue;
            }

            try {
                List<ScrapedListing> highMatchListings = scrapedListingRepository
                        .findByUserIdAndMatchTierAndFirstSeenAtAfter(user.getId(), MatchTier.HIGH, since);

                if (highMatchListings.isEmpty()) {
                    log.info("[EmailDigest] Usuário '{}' ({}) não possui anúncios HIGH match nas últimas {}h. Nenhum e-mail enviado.",
                            user.getUsername(), user.getEmail(), windowHours);
                    skippedNoListings++;
                    continue;
                }

                String appUrl = properties.getAppUrl();
                int maxItems = properties.getEmail().getDigest().getMaxItems();

                log.info("[EmailDigest] Usuário '{}' possui {} anúncios HIGH match encontrados. Montando digest (Top {} no template, link: {})...",
                        user.getUsername(), highMatchListings.size(), maxItems, appUrl);

                MonitoringEmailDigestNotificationTemplate template =
                        new MonitoringEmailDigestNotificationTemplate(user, highMatchListings, windowHours, appUrl, maxItems);

                NotificationEvent event = NotificationEvent.builder()
                        .recipient(user)
                        .contentTemplate(template)
                        .build();

                eventPublisher.publishEvent(event);
                digestsDispatched++;

                log.info("[EmailDigest] Evento de notificação publicado com sucesso para usuário '{}' ({}) com {} anúncios.",
                        user.getUsername(), user.getEmail(), highMatchListings.size());

            } catch (Exception ex) {
                log.error("[EmailDigest] Erro ao processar resumo de e-mail para usuário '{}' (ID: {}): {}",
                        user.getUsername(), user.getId(), ex.getMessage(), ex);
            }
        }

        log.info("[EmailDigest] Ciclo de e-mails periódicos finalizado. Total avaliados: {}, Digests disparados: {}, Sem novos anúncios: {}, Desativados/Inválidos: {}.",
                configs.size(), digestsDispatched, skippedNoListings, skippedDisabledOrInvalid);
    }
}
