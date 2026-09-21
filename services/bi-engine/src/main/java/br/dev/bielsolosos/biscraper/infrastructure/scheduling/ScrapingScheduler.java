package br.dev.bielsolosos.biscraper.infrastructure.scheduling;

import br.dev.bielsolosos.biscraper.domain.monitoring.service.ScrapingSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agendador da camada de infraestrutura responsável por disparar periodicamente a rotina de scraping.
 * 
 * Este componente atua como o gatilho de execução (driver/trigger) de infraestrutura,
 * sendo acionado periodicamente por um cronjob / task scheduler do Spring e delegando
 * toda a regra de negócio e orquestração de busca para o {@link ScrapingSchedulerService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScrapingScheduler {

    private final ScrapingSchedulerService scrapingSchedulerService;

    /**
     * Gatilho de execução acionado periodicamente por um cronjob (a cada 60 segundos).
     * 
     * Executado em segundo plano com taxa fixa de 60 segundos (1 minuto) e delay inicial de 15 segundos.
     * Invoca o serviço de domínio para verificar e despachar os monitores que estão no momento de raspagem.
     */
    @Scheduled(fixedRate = 60000, initialDelay = 15000)
    public void runScrapingCronJob() {
        log.trace("Cronjob de agendamento de scraping disparado pela infraestrutura.");
        scrapingSchedulerService.executeScheduledScrapes();
    }
}
