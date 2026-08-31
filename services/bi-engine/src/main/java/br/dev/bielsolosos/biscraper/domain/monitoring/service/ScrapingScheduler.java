package br.dev.bielsolosos.biscraper.domain.monitoring.service;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.MonitorSearchQuery;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ProductMonitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapingScheduler {

    private final ProductMonitorRepository productMonitorRepository;
    private final ScrapingJobDispatcher scrapingJobDispatcher;

    // Roda a cada 60 segundos (1 minuto) buscando monitores ativos e avaliando crons
    @Transactional
    @Scheduled(fixedRate = 60000, initialDelay = 15000)
    public void scheduleScrapes() {
        List<ProductMonitor> activeMonitors = productMonitorRepository.findByActiveTrue();
        if (activeMonitors.isEmpty()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        log.debug(">>> SCHEDULER: Verificando agendamento de {} monitor(es) ativo(s) em {} <<<", activeMonitors.size(), now);

        for (ProductMonitor monitor : activeMonitors) {
            if (isDue(monitor, now)) {
                log.info(">>> SCHEDULER: Disparando execução agendada para o monitor '{}' (ID: {}) [Cron: '{}'] <<<",
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

