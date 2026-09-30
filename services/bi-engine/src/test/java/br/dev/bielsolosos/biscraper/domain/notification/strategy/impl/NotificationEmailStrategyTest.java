package br.dev.bielsolosos.biscraper.domain.notification.strategy.impl;

import br.dev.bielsolosos.biscraper.core.enums.NotificationChannel;
import br.dev.bielsolosos.biscraper.core.utils.NotificationTemplate;
import br.dev.bielsolosos.biscraper.domain.notification.event.NotificationEvent;
import br.dev.bielsolosos.biscraper.domain.notification.model.NotificationLog;
import br.dev.bielsolosos.biscraper.domain.notification.repository.NotificationLogRepository;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.model.UserConfig;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserConfigRepository;
import br.dev.bielsolosos.biscraper.infrastructure.BiScraperProperties;
import br.dev.bielsolosos.biscraper.infrastructure.client.email.CloudflareEmailHttpClient;
import br.dev.bielsolosos.biscraper.infrastructure.client.email.dto.EmailSendRequest;
import br.dev.bielsolosos.biscraper.infrastructure.client.email.dto.EmailSendResponse;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEmailStrategyTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private UserConfigRepository userConfigRepository;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private CloudflareEmailHttpClient cloudflareEmailHttpClient;

    @Mock
    private NotificationTemplate notificationTemplate;

    private BiScraperProperties properties;
    private NotificationEmailStrategy strategy;

    @BeforeEach
    void setUp() {
        properties = new BiScraperProperties();
        properties.getEmail().setEnabled(true);
        properties.getEmail().setProvider(BiScraperProperties.Email.EmailProvider.SMTP);
        properties.getEmail().setFrom("BI Scraper <test@bi.com>");

        strategy = new NotificationEmailStrategy(
                notificationLogRepository,
                userConfigRepository,
                properties,
                javaMailSender,
                cloudflareEmailHttpClient
        );
    }

    @Test
    @DisplayName("Deve retornar EMAIL como canal suportado")
    void shouldReturnEmailChannel() {
        assertEquals(NotificationChannel.EMAIL, strategy.getChannel());
    }

    @Test
    @DisplayName("Deve enviar via SMTP quando provider for SMTP")
    void shouldSendViaSmtpSuccessfully() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("biel").email("biel@test.com").build();
        UserConfig config = UserConfig.builder().user(user).emailEnabled(true).build();

        NotificationEvent event = NotificationEvent.builder()
                .recipient(user)
                .contentTemplate(notificationTemplate)
                .build();

        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(userConfigRepository.findByUserId(userId)).thenReturn(Optional.of(config));
        when(javaMailSender.createMimeMessage()).thenReturn(mockMimeMessage);
        when(notificationTemplate.getSubject()).thenReturn("Teste SMTP");
        when(notificationTemplate.toHtmlEmail()).thenReturn("<p>HTML</p>");
        when(notificationTemplate.getMessageTemplate()).thenReturn("Texto");

        strategy.sendNotification(event);

        verify(javaMailSender, times(1)).send(mockMimeMessage);
        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, times(1)).save(logCaptor.capture());

        NotificationLog savedLog = logCaptor.getValue();
        assertNotNull(savedLog);
        assertEquals(user, savedLog.getRecipient());
        assertEquals(NotificationChannel.EMAIL, savedLog.getNotificationChannel());
    }

    @Test
    @DisplayName("Deve enviar via Cloudflare Worker quando provider for WORKER")
    void shouldSendViaWorkerSuccessfully() {
        properties.getEmail().setProvider(BiScraperProperties.Email.EmailProvider.WORKER);

        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("biel").email("biel@test.com").build();
        UserConfig config = UserConfig.builder().user(user).emailEnabled(true).build();

        NotificationEvent event = NotificationEvent.builder()
                .recipient(user)
                .contentTemplate(notificationTemplate)
                .build();

        when(userConfigRepository.findByUserId(userId)).thenReturn(Optional.of(config));
        when(notificationTemplate.getSubject()).thenReturn("Teste Worker");
        when(notificationTemplate.toHtmlEmail()).thenReturn("<p>HTML</p>");
        when(notificationTemplate.getMessageTemplate()).thenReturn("Texto");
        when(cloudflareEmailHttpClient.sendEmail(any(EmailSendRequest.class)))
                .thenReturn(new EmailSendResponse(true, "msg-123", null));

        strategy.sendNotification(event);

        verify(cloudflareEmailHttpClient, times(1)).sendEmail(any(EmailSendRequest.class));
        verify(notificationLogRepository, times(1)).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("Não deve enviar se o usuário tiver emailEnabled = false")
    void shouldNotSendIfEmailDisabledForUser() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("biel").email("biel@test.com").build();
        UserConfig config = UserConfig.builder().user(user).emailEnabled(false).build();

        NotificationEvent event = NotificationEvent.builder()
                .recipient(user)
                .build();

        when(userConfigRepository.findByUserId(userId)).thenReturn(Optional.of(config));

        strategy.sendNotification(event);

        verifyNoInteractions(javaMailSender);
        verifyNoInteractions(cloudflareEmailHttpClient);
        verifyNoInteractions(notificationLogRepository);
    }
}
