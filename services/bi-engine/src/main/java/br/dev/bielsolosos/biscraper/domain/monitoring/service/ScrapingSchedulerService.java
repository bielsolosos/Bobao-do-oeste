package br.dev.bielsolosos.biscraper.domain.monitoring.service;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.MonitorSearchQuery;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ProductMonitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Serviço de domínio responsável pela lógica de verificação e disparo das raspagens agendadas.
 * 
 * Este serviço contém as regras de negócio de agendamento (validação de expressões cron,
 * verificação de monitores devidos e atualização de timestamps de execução), sendo invocado
 * periodicamente por um cronjob / agendador na camada de infraestrutura.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapingSchedulerService {

    private final ProductMonitorRepository productMonitorRepository;
    private final ScrapingJobDispatcher scrapingJobDispatcher;

    /**
     * Processa todos os monitores de produto ativos e dispara a raspagem para aqueles
     * cuja expressão cron indica que a execução está no momento devido.
     * 
     * Observação: Este método é acionado periodicamente por um cronjob na camada de infraestrutura.
     */
    @Transactional
    public void executeScheduledScrapes() {
        List<ProductMonitor> activeMonitors = productMonitorRepository.findByActiveTrue();
        if (activeMonitors.isEmpty()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        log.debug(">>> SCHEDULER SERVICE: Verificando agendamento de {} monitor(es) ativo(s) em {} <<<", activeMonitors.size(), now);

        for (ProductMonitor monitor : activeMonitors) {
            if (isDue(monitor, now)) {
                log.info(">>> SCHEDULER SERVICE: Disparando execução agendada para o monitor '{}' (ID: {}) [Cron: '{}'] <<<",
                        monitor.getName(), monitor.getId(), monitor.getCronExpression());

                monitor.setLastScrapedAt(now);
                productMonitorRepository.save(monitor);

                for (MonitorSearchQuery query : monitor.getSearchQueries()) {
                    if (query.isActive()) {
                        scrapingJobDispatcher.dispatchQuery(monitor, query);
                    }
                }
            }
        }
    }

    /**
     * Valida se um monitor de produto está no momento de ser executado com base na sua expressão cron
     * e na última execução (ou data de criação).
     */
    public boolean isDue(ProductMonitor monitor, OffsetDateTime now) {
        String cronExpr = monitor.getCronExpression();
        if (cronExpr == null || cronExpr.isBlank()) {
            return false;
        }

        if (!CronExpression.isValidExpression(cronExpr)) {
            log.warn("Monitor '{}' (ID: {}) possui expressão cron inválida: '{}'",
                    monitor.getName(), monitor.getId(), cronExpr);
            return false;
        }

        CronExpression cron = CronExpression.parse(cronExpr);
        OffsetDateTime referenceTime = monitor.getLastScrapedAt() != null
                ? monitor.getLastScrapedAt()
                : monitor.getCreatedAt();

        if (referenceTime == null) {
            return true;
        }

        OffsetDateTime nextExecution = cron.next(referenceTime);
        return nextExecution != null && !nextExecution.isAfter(now);
    }
}
