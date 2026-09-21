package br.dev.bielsolosos.biscraper.domain.monitoring.service;

import br.dev.bielsolosos.biscraper.core.enums.ScrapingFrequency;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.MonitorSearchQuery;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ProductMonitorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScrapingSchedulerServiceTest {

    @Mock
    private ProductMonitorRepository productMonitorRepository;

    @Mock
    private ScrapingJobDispatcher scrapingJobDispatcher;

    @InjectMocks
    private ScrapingSchedulerService schedulerService;

    @Test
    @DisplayName("Deve disparar apenas queries ativas para monitores com cron vencido")
    void executeScheduledScrapesDueMonitor() {
        ProductMonitor monitor = ProductMonitor.builder()
                .id(UUID.randomUUID())
                .name("ThinkPad Monitor")
                .active(true)
                .cronExpression(ScrapingFrequency.EVERY_MINUTE.getCronExpression())
                .lastScrapedAt(OffsetDateTime.now().minusMinutes(5))
                .build();

        MonitorSearchQuery activeQuery = MonitorSearchQuery.builder()
                .id(UUID.randomUUID())
                .queryTerm("thinkpad t480")
                .active(true)
                .build();

        MonitorSearchQuery inactiveQuery = MonitorSearchQuery.builder()
                .id(UUID.randomUUID())
                .queryTerm("thinkpad t490")
                .active(false)
                .build();

        monitor.addSearchQuery(activeQuery);
        monitor.addSearchQuery(inactiveQuery);

        when(productMonitorRepository.findByActiveTrue()).thenReturn(List.of(monitor));

        schedulerService.executeScheduledScrapes();

        verify(scrapingJobDispatcher, times(1)).dispatchQuery(monitor, activeQuery);
        verify(scrapingJobDispatcher, never()).dispatchQuery(monitor, inactiveQuery);
        verify(productMonitorRepository, times(1)).save(monitor);
    }

    @Test
    @DisplayName("Não deve disparar monitor cujo cron ainda não venceu")
    void executeScheduledScrapesNotDueMonitor() {
        ProductMonitor monitor = ProductMonitor.builder()
                .id(UUID.randomUUID())
                .name("ThinkPad Monitor Daily")
                .active(true)
                .cronExpression(ScrapingFrequency.DAILY.getCronExpression())
                .lastScrapedAt(OffsetDateTime.now().minusMinutes(5))
                .build();

        MonitorSearchQuery query = MonitorSearchQuery.builder()
                .id(UUID.randomUUID())
                .queryTerm("thinkpad t480")
                .active(true)
                .build();

        monitor.addSearchQuery(query);

        when(productMonitorRepository.findByActiveTrue()).thenReturn(List.of(monitor));

        schedulerService.executeScheduledScrapes();

        verifyNoInteractions(scrapingJobDispatcher);
        verify(productMonitorRepository, never()).save(any(ProductMonitor.class));
    }

    @Test
    @DisplayName("Não deve disparar monitor com cronExpression nula ou em branco (manual)")
    void executeScheduledScrapesManualMonitor() {
        ProductMonitor monitor = ProductMonitor.builder()
                .id(UUID.randomUUID())
                .name("Manual Monitor")
                .active(true)
                .cronExpression(null)
                .build();

        when(productMonitorRepository.findByActiveTrue()).thenReturn(List.of(monitor));

        schedulerService.executeScheduledScrapes();

        verifyNoInteractions(scrapingJobDispatcher);
    }

    @Test
    @DisplayName("Não deve disparar monitor com cron inválido")
    void executeScheduledScrapesInvalidCron() {
        ProductMonitor monitor = ProductMonitor.builder()
                .id(UUID.randomUUID())
                .name("Invalid Cron Monitor")
                .active(true)
                .cronExpression("expressao_invalida")
                .build();

        when(productMonitorRepository.findByActiveTrue()).thenReturn(List.of(monitor));

        schedulerService.executeScheduledScrapes();

        verifyNoInteractions(scrapingJobDispatcher);
    }

    @Test
    @DisplayName("Não deve fazer nada quando não houver monitores ativos")
    void executeScheduledScrapesNoActiveMonitors() {
        when(productMonitorRepository.findByActiveTrue()).thenReturn(Collections.emptyList());

        schedulerService.executeScheduledScrapes();

        verifyNoInteractions(scrapingJobDispatcher);
    }

    @Test
    @DisplayName("isDue deve retornar true quando o monitor nunca foi executado")
    void isDueNeverExecuted() {
        ProductMonitor monitor = ProductMonitor.builder()
                .id(UUID.randomUUID())
                .name("New Monitor")
                .cronExpression(ScrapingFrequency.EVERY_MINUTE.getCronExpression())
                .createdAt(OffsetDateTime.now().minusMinutes(2))
                .lastScrapedAt(null)
                .build();

        boolean due = schedulerService.isDue(monitor, OffsetDateTime.now());

        assertTrue(due);
    }

    @Test
    @DisplayName("isDue deve retornar false para cron nulo ou inválido")
    void isDueInvalidOrNull() {
        ProductMonitor monitorNull = ProductMonitor.builder().cronExpression(null).build();
        ProductMonitor monitorInvalid = ProductMonitor.builder().cronExpression("abc").build();

        assertFalse(schedulerService.isDue(monitorNull, OffsetDateTime.now()));
        assertFalse(schedulerService.isDue(monitorInvalid, OffsetDateTime.now()));
    }
}
