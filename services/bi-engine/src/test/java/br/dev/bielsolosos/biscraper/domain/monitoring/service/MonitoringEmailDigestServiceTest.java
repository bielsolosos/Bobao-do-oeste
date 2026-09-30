package br.dev.bielsolosos.biscraper.domain.monitoring.service;

import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapedListing;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ScrapedListingRepository;
import br.dev.bielsolosos.biscraper.domain.notification.event.NotificationEvent;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.model.UserConfig;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserConfigRepository;
import br.dev.bielsolosos.biscraper.infrastructure.BiScraperProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringEmailDigestServiceTest {

    @Mock
    private UserConfigRepository userConfigRepository;

    @Mock
    private ScrapedListingRepository scrapedListingRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private BiScraperProperties properties;

    @InjectMocks
    private MonitoringEmailDigestService emailDigestService;

    @Captor
    private ArgumentCaptor<NotificationEvent> eventCaptor;

    private BiScraperProperties.Email emailProps;

    @BeforeEach
    void setUp() {
        emailProps = new BiScraperProperties.Email();
        emailProps.setEnabled(true);
        emailProps.getDigest().setWindowHours(4);
    }

    @Test
    @DisplayName("Deve ignorar envio quando e-mails estiverem desabilitados globalmente")
    void shouldIgnoreWhenEmailsGloballyDisabled() {
        emailProps.setEnabled(false);
        when(properties.getEmail()).thenReturn(emailProps);

        emailDigestService.sendEmailDigests();

        verifyNoInteractions(userConfigRepository, scrapedListingRepository, eventPublisher);
    }

    @Test
    @DisplayName("Deve disparar evento de notificação de e-mail quando usuário possuir anúncios HIGH match")
    void shouldPublishNotificationEventForUserWithHighMatchListings() {
        when(properties.getEmail()).thenReturn(emailProps);
        when(properties.getAppUrl()).thenReturn("https://bi.dev");

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("bielsolosos")
                .email("biel@dev.com")
                .active(true)
                .build();

        UserConfig config = UserConfig.builder()
                .id(UUID.randomUUID())
                .user(user)
                .emailEnabled(true)
                .build();

        when(userConfigRepository.findAll()).thenReturn(List.of(config));

        ScrapedListing listing = ScrapedListing.builder()
                .id(UUID.randomUUID())
                .productMonitor(ProductMonitor.builder().id(UUID.randomUUID()).name("Macbook").build())
                .title("Macbook Air M2")
                .currentPrice(BigDecimal.valueOf(4500))
                .matchTier(MatchTier.HIGH)
                .matchScore(BigDecimal.valueOf(95.0))
                .build();

        when(scrapedListingRepository.findByUserIdAndMatchTierAndCreatedAtAfter(eq(userId), eq(MatchTier.HIGH), any(OffsetDateTime.class)))
                .thenReturn(List.of(listing));

        emailDigestService.sendEmailDigests();

        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        NotificationEvent publishedEvent = eventCaptor.getValue();
        assertNotNull(publishedEvent);
        assertEquals(user, publishedEvent.getRecipient());
    }

    @Test
    @DisplayName("Não deve disparar evento se o usuário não possuir anúncios HIGH match recentes")
    void shouldNotPublishEventWhenNoListingsFound() {
        when(properties.getEmail()).thenReturn(emailProps);

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("bielsolosos")
                .email("biel@dev.com")
                .active(true)
                .build();

        UserConfig config = UserConfig.builder()
                .id(UUID.randomUUID())
                .user(user)
                .emailEnabled(true)
                .build();

        when(userConfigRepository.findAll()).thenReturn(List.of(config));
        when(scrapedListingRepository.findByUserIdAndMatchTierAndCreatedAtAfter(eq(userId), eq(MatchTier.HIGH), any(OffsetDateTime.class)))
                .thenReturn(Collections.emptyList());

        emailDigestService.sendEmailDigests();

        verify(eventPublisher, never()).publishEvent(any());
    }
}
