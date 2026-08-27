package br.dev.bielsolosos.biscraper.domain.monitoring.service;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.MonitorSearchQuery;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ProductMonitorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScrapingSchedulerTest {

    @Mock
    private ProductMonitorRepository productMonitorRepository;

    @Mock
    private ScrapingJobDispatcher scrapingJobDispatcher;

    @InjectMocks
    private ScrapingScheduler scheduler;

    @Test
    @DisplayName("Deve disparar queries para monitores ativos encontrados no ciclo de agendamento")
    void scheduleScrapesActiveMonitors() {
        ProductMonitor monitor = ProductMonitor.builder()
                .id(UUID.randomUUID())
                .name("ThinkPad Monitor")
                .build();

        MonitorSearchQuery query = MonitorSearchQuery.builder()
                .id(UUID.randomUUID())
                .queryTerm("thinkpad t480")
                .build();

        monitor.addSearchQuery(query);

        when(productMonitorRepository.findByActiveTrue()).thenReturn(List.of(monitor));

        scheduler.scheduleScrapes();

        verify(scrapingJobDispatcher, times(1)).dispatchQuery(monitor, query);
    }

    @Test
    @DisplayName("Não deve fazer nada quando não houver monitores ativos")
    void scheduleScrapesNoActiveMonitors() {
        when(productMonitorRepository.findByActiveTrue()).thenReturn(Collections.emptyList());

        scheduler.scheduleScrapes();

        verifyNoInteractions(scrapingJobDispatcher);
    }
}
