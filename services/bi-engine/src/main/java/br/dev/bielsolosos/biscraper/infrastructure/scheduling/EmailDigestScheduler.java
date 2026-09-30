package br.dev.bielsolosos.biscraper.infrastructure.scheduling;

import br.dev.bielsolosos.biscraper.domain.monitoring.service.MonitoringEmailDigestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agendador da camada de infraestrutura responsável por disparar periodicamente
 * o envio de resumos (digests) de anúncios HIGH match por e-mail.
 *
 * Atua como o trigger do Spring Scheduling (cronjob) delegando a execução
 * para o serviço de domínio {@link MonitoringEmailDigestService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailDigestScheduler {

    private final MonitoringEmailDigestService monitoringEmailDigestService;

    /**
     * Cronjob executado 4x ao dia (padrão: 08:00, 12:00, 16:00, 20:00).
     */
    @Scheduled(cron = "${biscraper.email.digest.cron:0 0 8,12,16,20 * * *}")
    public void runEmailDigestCronJob() {
        log.trace("Cronjob de digest de e-mail disparado pela infraestrutura.");
        monitoringEmailDigestService.sendEmailDigests();
    }
}
