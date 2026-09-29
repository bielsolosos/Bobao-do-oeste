package br.dev.bielsolosos.biscraper.domain.ai.service;

import br.dev.bielsolosos.biscraper.domain.ai.model.AiAnalysisLog;
import br.dev.bielsolosos.biscraper.domain.ai.model.dto.AiAnalysisLogCreateDto;
import br.dev.bielsolosos.biscraper.domain.ai.model.dto.AiAnalysisLogResponse;
import br.dev.bielsolosos.biscraper.domain.ai.repository.AiAnalysisLogRepository;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapingExecution;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.WebhookEvent;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ProductMonitorRepository;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ScrapedListingRepository;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.service.MeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAnalysisLogServiceTest {

    @Mock
    private AiAnalysisLogRepository aiAnalysisLogRepository;

    @Mock
    private ProductMonitorRepository productMonitorRepository;

    @Mock
    private ScrapedListingRepository scrapedListingRepository;

    @Mock
    private MeService meService;

    @InjectMocks
    private AiAnalysisLogService aiAnalysisLogService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .build();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Deve salvar AiAnalysisLog capturando requestId e jobId de DTO explicitamente")
    void shouldSaveLogWithExplicitRequestIdAndJobId() {
        AiAnalysisLogCreateDto dto = AiAnalysisLogCreateDto.builder()
                .requestId("req-explicit-123")
                .jobId("job-explicit-456")
                .modelName("gemini-2.0-flash")
                .vendor("GEMINI")
                .status("SUCCESS")
                .itemsCount(5)
                .build();

        aiAnalysisLogService.saveLog(dto);

        ArgumentCaptor<AiAnalysisLog> captor = ArgumentCaptor.forClass(AiAnalysisLog.class);
        verify(aiAnalysisLogRepository).save(captor.capture());

        AiAnalysisLog saved = captor.getValue();
        assertEquals("req-explicit-123", saved.getRequestId());
        assertEquals("job-explicit-456", saved.getJobId());
        assertEquals("gemini-2.0-flash", saved.getModelName());
    }

    @Test
    @DisplayName("Deve salvar AiAnalysisLog resolvendo requestId e jobId a partir da ScrapingExecution e WebhookEvent")
    void shouldSaveLogResolvingFromScrapingExecution() {
        WebhookEvent webhookEvent = WebhookEvent.builder()
                .requestId("req-from-event-999")
                .jobId("job-from-event-888")
                .build();

        ScrapingExecution execution = ScrapingExecution.builder()
                .webhookEvent(webhookEvent)
                .build();

        AiAnalysisLogCreateDto dto = AiAnalysisLogCreateDto.builder()
                .scrapingExecution(execution)
                .modelName("deepseek-chat")
                .vendor("DEEPSEEK")
                .status("SUCCESS")
                .build();

        aiAnalysisLogService.saveLog(dto);

        ArgumentCaptor<AiAnalysisLog> captor = ArgumentCaptor.forClass(AiAnalysisLog.class);
        verify(aiAnalysisLogRepository).save(captor.capture());

        AiAnalysisLog saved = captor.getValue();
        assertEquals("req-from-event-999", saved.getRequestId());
        assertEquals("job-from-event-888", saved.getJobId());
        assertEquals("DEEPSEEK", saved.getVendor());
    }

    @Test
    @DisplayName("Deve salvar AiAnalysisLog com erro resolvendo requestId e jobId do MDC quando dto e execution forem nulos")
    void shouldSaveLogResolvingFromMdcOnError() {
        MDC.put("requestId", "mdc-req-777");
        MDC.put("jobId", "mdc-job-555");

        AiAnalysisLogCreateDto errorDto = AiAnalysisLogCreateDto.builder()
                .modelName("gemini-2.0-pro")
                .status("ERROR")
                .errorMessage("Timeout connection")
                .build();

        aiAnalysisLogService.saveLog(errorDto);

        ArgumentCaptor<AiAnalysisLog> captor = ArgumentCaptor.forClass(AiAnalysisLog.class);
        verify(aiAnalysisLogRepository).save(captor.capture());

        AiAnalysisLog saved = captor.getValue();
        assertEquals("mdc-req-777", saved.getRequestId());
        assertEquals("mdc-job-555", saved.getJobId());
        assertEquals("ERROR", saved.getStatus());
        assertEquals("Timeout connection", saved.getErrorMessage());
    }

    @Test
    @DisplayName("Deve listar todos os logs do usuário logado")
    @SuppressWarnings("unchecked")
    void shouldListAllLogsForCurrentUser() {
        when(meService.getMe()).thenReturn(currentUser);

        AiAnalysisLog logItem = AiAnalysisLog.builder()
                .id(UUID.randomUUID())
                .requestId("req-1")
                .jobId("job-1")
                .modelName("gemini-2.0-flash")
                .vendor("GEMINI")
                .status("SUCCESS")
                .build();

        Page<AiAnalysisLog> page = new PageImpl<>(List.of(logItem));
        when(aiAnalysisLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<AiAnalysisLogResponse> result = aiAnalysisLogService.listAllAiLogs("all", null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("req-1", result.getContent().get(0).requestId());
        assertEquals("job-1", result.getContent().get(0).jobId());
    }
}
