package br.dev.bielsolosos.biscraper.infrastructure.scheduling;

import br.dev.bielsolosos.biscraper.domain.monitoring.service.ScrapingSchedulerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScrapingSchedulerTest {

    @Mock
    private ScrapingSchedulerService scrapingSchedulerService;

    @InjectMocks
    private ScrapingScheduler scrapingScheduler;

    @Test
    @DisplayName("Deve delegar a execução do agendamento para o serviço de domínio ao acionar o cronjob")
    void shouldDelegateExecutionToDomainService() {
        scrapingScheduler.runScrapingCronJob();

        verify(scrapingSchedulerService, times(1)).executeScheduledScrapes();
    }
}
