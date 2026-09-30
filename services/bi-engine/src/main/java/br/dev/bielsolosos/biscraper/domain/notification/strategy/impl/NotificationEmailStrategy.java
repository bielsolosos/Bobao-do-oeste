package br.dev.bielsolosos.biscraper.domain.notification.strategy.impl;

import br.dev.bielsolosos.biscraper.core.enums.NotificationChannel;
import br.dev.bielsolosos.biscraper.core.utils.NotificationTemplate;
import br.dev.bielsolosos.biscraper.domain.notification.event.NotificationEvent;
import br.dev.bielsolosos.biscraper.domain.notification.repository.NotificationLogRepository;
import br.dev.bielsolosos.biscraper.domain.notification.strategy.NotificationStrategy;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.model.UserConfig;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserConfigRepository;
import br.dev.bielsolosos.biscraper.infrastructure.BiScraperProperties;
import br.dev.bielsolosos.biscraper.infrastructure.client.email.CloudflareEmailHttpClient;
import br.dev.bielsolosos.biscraper.infrastructure.client.email.dto.EmailSendRequest;
import br.dev.bielsolosos.biscraper.infrastructure.client.email.dto.EmailSendResponse;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class NotificationEmailStrategy extends NotificationStrategy {

    private final UserConfigRepository userConfigRepository;
    private final BiScraperProperties properties;
    private final JavaMailSender javaMailSender;
    private final CloudflareEmailHttpClient cloudflareEmailHttpClient;

    public NotificationEmailStrategy(
            NotificationLogRepository notificationLogRepository,
            UserConfigRepository userConfigRepository,
            BiScraperProperties properties,
            JavaMailSender javaMailSender,
            CloudflareEmailHttpClient cloudflareEmailHttpClient
    ) {
        super(notificationLogRepository);
        this.userConfigRepository = userConfigRepository;
        this.properties = properties;
        this.javaMailSender = javaMailSender;
        this.cloudflareEmailHttpClient = cloudflareEmailHttpClient;
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void sendNotification(NotificationEvent event) {
        User recipient = event.getRecipient();
        if (recipient == null || StringUtils.isBlank(recipient.getEmail())) {
            log.warn("Tentativa de enviar notificação por e-mail sem destinatário válido.");
            return;
        }

        if (!properties.getEmail().isEnabled()) {
            log.info("Notificações por e-mail estão desativadas globalmente no sistema.");
            return;
        }

        Optional<UserConfig> configOpt = userConfigRepository.findByUserId(recipient.getId());
        if (configOpt.isEmpty() || !configOpt.get().isEmailEnabled()) {
            log.info("Notificações por e-mail estão desativadas para o usuário '{}'", recipient.getUsername());
            return;
        }

        try {
            NotificationTemplate template = event.getContentTemplate();
            String subject = template != null ? template.getSubject() : "BI Scraper - Notificação";
            String html = template != null ? template.toHtmlEmail() : "<p>Notificação BI Scraper</p>";
            String text = template != null ? template.getMessageTemplate() : "Notificação BI Scraper";

            BiScraperProperties.Email.EmailProvider provider = properties.getEmail().getProvider();

            if (provider == BiScraperProperties.Email.EmailProvider.WORKER) {
                sendViaCloudflareWorker(recipient.getEmail(), subject, html, text);
            } else {
                sendViaSmtp(recipient.getEmail(), subject, html, text);
            }

            String logDescription = String.format("E-mail (%s) enviado com sucesso para '%s' com assunto: '%s'.",
                    provider, recipient.getEmail(), subject);
            saveLog(event, logDescription);
            log.info("E-mail entregue com sucesso para '{}' via provedor '{}'", recipient.getEmail(), provider);

        } catch (Exception e) {
            log.error("Erro ao enviar e-mail para usuário '{}' ({}): {}",
                    recipient.getUsername(), recipient.getEmail(), e.getMessage(), e);
            saveLog(event, "Falha ao enviar e-mail: " + e.getMessage());
        }
    }

    private void sendViaSmtp(String toEmail, String subject, String html, String text) throws Exception {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(properties.getEmail().getFrom());
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(text, html);

        javaMailSender.send(mimeMessage);
    }

    private void sendViaCloudflareWorker(String toEmail, String subject, String html, String text) {
        EmailSendRequest request = new EmailSendRequest(toEmail, subject, html, text);
        EmailSendResponse response = cloudflareEmailHttpClient.sendEmail(request);

        if (response != null && !response.success()) {
            throw new RuntimeException("Worker retornou erro: " + response.error());
        }
    }
}
