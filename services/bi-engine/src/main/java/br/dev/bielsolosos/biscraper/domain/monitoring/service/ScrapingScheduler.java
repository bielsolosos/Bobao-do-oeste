package br.dev.bielsolosos.biscraper.domain.monitoring.service;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.MonitorSearchQuery;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ProductMonitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class ScrapingScheduler {

    private final ProductMonitorRepository productMonitorRepository;
    private final ScrapingJobDispatcher scrapingJobDispatcher;

    // Roda a cada 60 segundos (1 minuto) buscando monitores ativos
    @Transactional(readOnly = true)
    @Scheduled(fixedRate = 60000, initialDelay = 15000)
    public void scheduleScrapes() {
        List<ProductMonitor> activeMonitors = productMonitorRepository.findByActiveTrue();
        if (activeMonitors.isEmpty()) {
            return;
        }

        log.info(">>> SCHEDULER: Executando ciclo de disparo para {} monitor(es) ativo(s)... <<<", activeMonitors.size());
        for (ProductMonitor monitor : activeMonitors) {
            for (MonitorSearchQuery query : monitor.getSearchQueries()) {
                scrapingJobDispatcher.dispatchQuery(monitor, query);
            }
        }
    }
}
