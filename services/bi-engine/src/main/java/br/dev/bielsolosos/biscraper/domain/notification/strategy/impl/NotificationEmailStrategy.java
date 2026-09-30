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

    public NotificationEmailStrategy(
            NotificationLogRepository notificationLogRepository,
            UserConfigRepository userConfigRepository,
            BiScraperProperties properties,
            JavaMailSender javaMailSender
    ) {
        super(notificationLogRepository);
        this.userConfigRepository = userConfigRepository;
        this.properties = properties;
        this.javaMailSender = javaMailSender;
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

            log.info("[NotificationEmail] Preparando envio de e-mail SMTP para '{}' (From: '{}') | Assunto: '{}'",
                    recipient.getEmail(), properties.getEmail().getFrom(), subject);

            sendViaSmtp(recipient.getEmail(), subject, html, text);

            String logDescription = String.format("E-mail (SMTP) enviado com sucesso para '%s' com assunto: '%s'.",
                    recipient.getEmail(), subject);
            saveLog(event, logDescription);
            log.info("[NotificationEmail] E-mail entregue com sucesso para '{}' via SMTP", recipient.getEmail());

        } catch (Exception e) {
            log.error("[NotificationEmail] Erro ao enviar e-mail para usuário '{}' ({}) via SMTP: {}",
                    recipient.getUsername(), recipient.getEmail(), e.getMessage(), e);
            saveLog(event, "Falha ao enviar e-mail: " + e.getMessage());
        }
    }

    private void sendViaSmtp(String toEmail, String subject, String html, String text) throws Exception {
        log.info("[NotificationEmail-SMTP] Montando MimeMessage para '{}' a partir de '{}' com codificação UTF-8",
                toEmail, properties.getEmail().getFrom());
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(properties.getEmail().getFrom());
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(text, html);

        javaMailSender.send(mimeMessage);
        log.info("[NotificationEmail-SMTP] MimeMessage despachado com sucesso para o servidor SMTP");
    }
}
