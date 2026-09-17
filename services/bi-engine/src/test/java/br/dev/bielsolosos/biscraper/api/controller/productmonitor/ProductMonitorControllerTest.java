package br.dev.bielsolosos.biscraper.api.controller.productmonitor;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.monitor.ProductMonitorRequest;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.monitor.ProductMonitorResponse;
import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.core.enums.ScrapingFrequency;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import br.dev.bielsolosos.biscraper.domain.monitoring.service.ProductMonitorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductMonitorControllerTest {

    @Mock
    private ProductMonitorService service;

    @Mock
    private br.dev.bielsolosos.biscraper.domain.ai.service.AiAnalysisLogService aiAnalysisLogService;

    @InjectMocks
    private ProductMonitorController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID monitorId;
    private ProductMonitorResponse response;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();

        monitorId = UUID.randomUUID();
        response = new ProductMonitorResponse(
                monitorId,
                "Monitor Gamer",
                "Descrição",
                AnalysisType.SIMPLE,
                Vendor.OLX,
                true,
                ScrapingFrequency.DAILY,
                ScrapingFrequency.DAILY.getCronExpression(),
                null,
                Collections.emptyList(),
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    @Test
    @DisplayName("POST /api/v1/product-monitors - Deve criar monitor e retornar 201 CREATED")
    void shouldCreateMonitor() throws Exception {
        ProductMonitorRequest request = new ProductMonitorRequest(
                "Monitor Gamer",
                "Descrição",
                Vendor.OLX,
                AnalysisType.SIMPLE,
                null,
                List.of("termo"),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(500),
                null,
                null,
                false,
                ScrapingFrequency.DAILY
        );

        when(service.create(any(ProductMonitorRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/product-monitors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(monitorId.toString()))
                .andExpect(jsonPath("$.name").value("Monitor Gamer"));
    }

    @Test
    @DisplayName("GET /api/v1/product-monitors - Deve retornar lista paginada e 200 OK")
    void shouldListMonitors() throws Exception {
        when(service.listPaged(any())).thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/product-monitors?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(monitorId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/product-monitors/{id} - Deve retornar monitor por ID e 200 OK")
    void shouldGetMonitorById() throws Exception {
        when(service.getById(monitorId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/product-monitors/" + monitorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(monitorId.toString()));
    }

    @Test
    @DisplayName("PUT /api/v1/product-monitors/{id} - Deve atualizar monitor e retornar 200 OK")
    void shouldUpdateMonitor() throws Exception {
        ProductMonitorRequest request = new ProductMonitorRequest(
                "Monitor Gamer Atualizado",
                "Descrição",
                Vendor.OLX,
                AnalysisType.SIMPLE,
                null,
                List.of("termo"),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(500),
                null,
                null,
                false,
                ScrapingFrequency.DAILY
        );

        when(service.update(eq(monitorId), any(ProductMonitorRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/product-monitors/" + monitorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(monitorId.toString()));
    }

    @Test
    @DisplayName("PATCH /api/v1/product-monitors/{id}/deactivate - Deve desativar monitor e retornar 200 OK")
    void shouldDeactivateMonitor() throws Exception {
        when(service.deactivate(monitorId)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/product-monitors/" + monitorId + "/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(monitorId.toString()));
    }

    @Test
    @DisplayName("PATCH /api/v1/product-monitors/{id}/activate - Deve reativar monitor e retornar 200 OK")
    void shouldActivateMonitor() throws Exception {
        when(service.activate(monitorId)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/product-monitors/" + monitorId + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(monitorId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/product-monitors/frequencies - Deve listar todas as opções de frequências com 200 OK")
    void shouldListFrequencies() throws Exception {
        mockMvc.perform(get("/api/v1/product-monitors/frequencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.name == 'EIGHT_TIMES_DAILY')].description").value("8 vezes ao dia (a cada 3 horas)"))
                .andExpect(jsonPath("$[?(@.name == 'SIX_TIMES_DAILY')].description").value("6 vezes ao dia (a cada 4 horas)"))
                .andExpect(jsonPath("$[?(@.name == 'FOUR_TIMES_DAILY')].description").value("4 vezes ao dia (a cada 6 horas)"));
    }

    @Test
    @DisplayName("DELETE /api/v1/product-monitors/{id} - Deve excluir monitor e retornar 204 NO CONTENT")
    void shouldDeleteMonitor() throws Exception {
        doNothing().when(service).delete(monitorId);

        mockMvc.perform(delete("/api/v1/product-monitors/" + monitorId))
                .andExpect(status().isNoContent());

        verify(service, times(1)).delete(monitorId);
    }
}
