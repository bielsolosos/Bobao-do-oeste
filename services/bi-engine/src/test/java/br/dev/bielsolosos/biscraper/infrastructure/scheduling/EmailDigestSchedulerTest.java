package br.dev.bielsolosos.biscraper.infrastructure.scheduling;

import br.dev.bielsolosos.biscraper.domain.monitoring.service.MonitoringEmailDigestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailDigestSchedulerTest {

    @Mock
    private MonitoringEmailDigestService monitoringEmailDigestService;

    @InjectMocks
    private EmailDigestScheduler emailDigestScheduler;

    @Test
    @DisplayName("Deve delegar a execução do digest de e-mails para o serviço de domínio ao acionar o cronjob")
    void shouldDelegateExecutionToDomainService() {
        emailDigestScheduler.runEmailDigestCronJob();

        verify(monitoringEmailDigestService, times(1)).sendEmailDigests();
    }
}
