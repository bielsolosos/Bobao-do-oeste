package br.dev.bielsolosos.biscraper.domain.monitoring.service;

import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.core.exception.BusinessException;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics.*;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ProductMonitorRepository;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ScrapedListingRepository;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.service.MeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorMetricsServiceTest {

    @Mock
    private ScrapedListingRepository scrapedListingRepository;

    @Mock
    private ProductMonitorRepository productMonitorRepository;

    @Mock
    private MeService meService;

    @InjectMocks
    private MonitorMetricsService metricsService;

    private User user;
    private ProductMonitor monitor;
    private UUID userId;
    private UUID monitorId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        monitorId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .username("bielsolosos")
                .email("biel@dev.com")
                .build();

        monitor = ProductMonitor.builder()
                .id(monitorId)
                .name("Monitor Gamer")
                .user(user)
                .lastScrapedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve retornar overview global quando monitorId for nulo")
    void shouldReturnGlobalOverview() {
        when(meService.getMe()).thenReturn(user);
        when(scrapedListingRepository.getOverviewStatsByUserId(userId)).thenReturn(
                new OverviewStatsDto(50L, 15L, BigDecimal.valueOf(1000), BigDecimal.valueOf(5000), 2500.0)
        );
        when(productMonitorRepository.findByUserId(userId)).thenReturn(List.of(monitor));

        MetricsOverviewResponse response = metricsService.getOverview(null);

        assertNotNull(response);
        assertEquals(50L, response.totalListings());
        assertEquals(15L, response.highRelevanceCount());
        assertEquals(BigDecimal.valueOf(1000), response.minPrice());
        assertEquals(BigDecimal.valueOf(5000), response.maxPrice());
        assertEquals(BigDecimal.valueOf(2500.0).setScale(2), response.avgPrice());
        assertNotNull(response.lastScrapedAt());
    }

    @Test
    @DisplayName("Deve retornar overview por monitor específico")
    void shouldReturnMonitorOverview() {
        when(meService.getMe()).thenReturn(user);
        when(productMonitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(scrapedListingRepository.getOverviewStatsByMonitorId(monitorId)).thenReturn(
                new OverviewStatsDto(20L, 5L, BigDecimal.valueOf(1500), BigDecimal.valueOf(3500), 2000.0)
        );

        MetricsOverviewResponse response = metricsService.getOverview(monitorId);

        assertNotNull(response);
        assertEquals(20L, response.totalListings());
        assertEquals(5L, response.highRelevanceCount());
        assertEquals(BigDecimal.valueOf(1500), response.minPrice());
        assertEquals(BigDecimal.valueOf(3500), response.maxPrice());
    }

    @Test
    @DisplayName("Deve retornar contagem de tiers diretamente do repositório")
    void shouldReturnTiers() {
        when(meService.getMe()).thenReturn(user);
        when(scrapedListingRepository.getTierMetricsByUserId(userId)).thenReturn(
                new TierMetricsResponse(10L, 5L, 3L, 2L, 20L)
        );

        TierMetricsResponse response = metricsService.getTiers(null);

        assertNotNull(response);
        assertEquals(10L, response.high());
        assertEquals(5L, response.medium());
        assertEquals(3L, response.low());
        assertEquals(2L, response.none());
        assertEquals(20L, response.total());
    }

    @Test
    @DisplayName("Deve calcular faixas de preço através de PriceBucketCountsDto")
    void shouldReturnPriceDistribution() {
        when(meService.getMe()).thenReturn(user);
        when(scrapedListingRepository.getPriceBucketsByUserId(userId)).thenReturn(
                new PriceBucketCountsDto(2L, 1L, 1L, 1L, 1L)
        );

        PriceDistributionResponse response = metricsService.getPrices(null);

        assertNotNull(response);
        assertEquals(5, response.buckets().size());
        assertEquals(2L, response.buckets().get(0).count()); // <= 1500
        assertEquals(1L, response.buckets().get(1).count()); // 1500 - 3000
        assertEquals(1L, response.buckets().get(2).count()); // 3000 - 5000
        assertEquals(1L, response.buckets().get(3).count()); // 5000 - 8000
        assertEquals(1L, response.buckets().get(4).count()); // > 8000
    }

    @Test
    @DisplayName("Deve agrupar marcas extraídas e inferidas de título")
    void shouldReturnBrandDistribution() {
        when(meService.getMe()).thenReturn(user);

        br.dev.bielsolosos.biscraper.domain.monitoring.repository.projection.SpecsAndTitleProjection p1 = mockProjection(Map.of("brand", "DELL"), "Notebook Dell Inspiron");
        br.dev.bielsolosos.biscraper.domain.monitoring.repository.projection.SpecsAndTitleProjection p2 = mockProjection(Map.of("brand", "APPLE"), "Macbook Air M1");
        br.dev.bielsolosos.biscraper.domain.monitoring.repository.projection.SpecsAndTitleProjection p3 = mockProjection(Map.of(), "Notebook Lenovo Thinkpad T480");
        br.dev.bielsolosos.biscraper.domain.monitoring.repository.projection.SpecsAndTitleProjection p4 = mockProjection(null, "Notebook Acer Nitro 5");

        when(scrapedListingRepository.findSpecsAndTitleByUserId(userId)).thenReturn(List.of(p1, p2, p3, p4));

        BrandDistributionResponse response = metricsService.getBrands(null);

        assertNotNull(response);
        assertFalse(response.brands().isEmpty());
        assertTrue(response.brands().stream().anyMatch(b -> b.brand().equals("DELL") && b.count() == 1));
        assertTrue(response.brands().stream().anyMatch(b -> b.brand().equals("APPLE") && b.count() == 1));
        assertTrue(response.brands().stream().anyMatch(b -> b.brand().equals("LENOVO") && b.count() == 1));
        assertTrue(response.brands().stream().anyMatch(b -> b.brand().equals("ACER") && b.count() == 1));
    }

    private br.dev.bielsolosos.biscraper.domain.monitoring.repository.projection.SpecsAndTitleProjection mockProjection(Map<String, Object> specs, String title) {
        return new br.dev.bielsolosos.biscraper.domain.monitoring.repository.projection.SpecsAndTitleProjection() {
            @Override
            public Map<String, Object> getExtractedSpecs() {
                return specs;
            }

            @Override
            public String getTitle() {
                return title;
            }
        };
    }

    @Test
    @DisplayName("Deve retornar timeline preenchida com datas")
    void shouldReturnTimeline() {
        when(meService.getMe()).thenReturn(user);

        br.dev.bielsolosos.biscraper.domain.monitoring.repository.projection.TimelineCountProjection t1 =
                new br.dev.bielsolosos.biscraper.domain.monitoring.repository.projection.TimelineCountProjection() {
                    @Override
                    public Object getDate() {
                        return java.time.LocalDate.now().minusDays(1);
                    }

                    @Override
                    public long getCount() {
                        return 8L;
                    }
                };

        when(scrapedListingRepository.countTimelineByUserId(eq(userId), any())).thenReturn(List.of(t1));

        TimelineMetricsResponse response = metricsService.getTimeline(null, 7);

        assertNotNull(response);
        assertEquals(7, response.points().size());
        assertTrue(response.points().stream().anyMatch(p -> p.count() == 8L));
    }

    @Test
    @DisplayName("Deve lançar exceção de acesso negado quando monitor pertencer a outro usuário")
    void shouldThrowExceptionWhenMonitorBelongsToAnotherUser() {
        User otherUser = User.builder().id(UUID.randomUUID()).username("other").build();
        ProductMonitor otherMonitor = ProductMonitor.builder().id(monitorId).user(otherUser).build();

        when(meService.getMe()).thenReturn(user);
        when(productMonitorRepository.findById(monitorId)).thenReturn(Optional.of(otherMonitor));

        assertThrows(BusinessException.class, () -> metricsService.getOverview(monitorId));
    }
}
