package br.dev.bielsolosos.biscraper.api.controller.productmonitor;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics.*;
import br.dev.bielsolosos.biscraper.domain.monitoring.service.MonitorMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductMonitorMetricsControllerTest {

    @Mock
    private MonitorMetricsService metricsService;

    @InjectMocks
    private ProductMonitorMetricsController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/v1/product-monitors/metrics/overview - Deve retornar 200 com métricas gerais")
    void shouldReturnOverview() throws Exception {
        MetricsOverviewResponse response = new MetricsOverviewResponse(
                100L, 25L, BigDecimal.valueOf(1000), BigDecimal.valueOf(5000), BigDecimal.valueOf(2800), OffsetDateTime.now()
        );
        when(metricsService.getOverview(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/product-monitors/metrics/overview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalListings").value(100))
                .andExpect(jsonPath("$.highRelevanceCount").value(25))
                .andExpect(jsonPath("$.minPrice").value(1000));
    }

    @Test
    @DisplayName("GET /api/v1/product-monitors/metrics/tiers - Deve retornar 200 com distribuição de tiers")
    void shouldReturnTiers() throws Exception {
        TierMetricsResponse response = new TierMetricsResponse(20, 15, 10, 5, 50);
        when(metricsService.getTiers(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/product-monitors/metrics/tiers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.high").value(20))
                .andExpect(jsonPath("$.total").value(50));
    }

    @Test
    @DisplayName("GET /api/v1/product-monitors/metrics/prices - Deve retornar 200 com faixas de preço")
    void shouldReturnPrices() throws Exception {
        PriceDistributionResponse response = new PriceDistributionResponse(List.of(
                new PriceDistributionResponse.PriceBucket("Até R$ 1.500", BigDecimal.ZERO, BigDecimal.valueOf(1500), 10)
        ));
        when(metricsService.getPrices(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/product-monitors/metrics/prices")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buckets[0].label").value("Até R$ 1.500"))
                .andExpect(jsonPath("$.buckets[0].count").value(10));
    }

    @Test
    @DisplayName("GET /api/v1/product-monitors/metrics/brands - Deve retornar 200 com distribuição de marcas")
    void shouldReturnBrands() throws Exception {
        BrandDistributionResponse response = new BrandDistributionResponse(List.of(
                new BrandDistributionResponse.BrandItem("DELL", 30)
        ));
        when(metricsService.getBrands(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/product-monitors/metrics/brands")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brands[0].brand").value("DELL"))
                .andExpect(jsonPath("$.brands[0].count").value(30));
    }

    @Test
    @DisplayName("GET /api/v1/product-monitors/metrics/timeline - Deve retornar 200 com série temporal")
    void shouldReturnTimeline() throws Exception {
        TimelineMetricsResponse response = new TimelineMetricsResponse(List.of(
                new TimelineMetricsResponse.TimelinePoint("2026-09-20", 5)
        ));
        when(metricsService.getTimeline(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/product-monitors/metrics/timeline")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points[0].date").value("2026-09-20"))
                .andExpect(jsonPath("$.points[0].count").value(5));
    }
}
