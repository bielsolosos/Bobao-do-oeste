package br.dev.bielsolosos.biscraper.api.controller.productmonitor;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics.*;
import br.dev.bielsolosos.biscraper.domain.monitoring.service.MonitorMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Product Monitor Metrics", description = "Endpoints modulares e granulares para dashboards e gráficos.")
@RestController
@RequestMapping("/api/v1/product-monitors/metrics")
@RequiredArgsConstructor
public class ProductMonitorMetricsController {

    private final MonitorMetricsService metricsService;

    @Operation(summary = "Obter KPIs gerais (total de anúncios, alta relevância, preços min/max/médio)")
    @GetMapping("/overview")
    public ResponseEntity<MetricsOverviewResponse> getOverview(
            @RequestParam(required = false) UUID monitorId) {
        return ResponseEntity.ok(metricsService.getOverview(monitorId));
    }

    @Operation(summary = "Obter distribuição de relevância (Match Tiers: HIGH, MEDIUM, LOW, NONE) para gráfico de rosquinha")
    @GetMapping("/tiers")
    public ResponseEntity<TierMetricsResponse> getTiers(
            @RequestParam(required = false) UUID monitorId) {
        return ResponseEntity.ok(metricsService.getTiers(monitorId));
    }

    @Operation(summary = "Obter histograma de faixas de preço para gráfico de barras")
    @GetMapping("/prices")
    public ResponseEntity<PriceDistributionResponse> getPrices(
            @RequestParam(required = false) UUID monitorId) {
        return ResponseEntity.ok(metricsService.getPrices(monitorId));
    }

    @Operation(summary = "Obter distribuição por marcas mais frequentes para gráfico de barras")
    @GetMapping("/brands")
    public ResponseEntity<BrandDistributionResponse> getBrands(
            @RequestParam(required = false) UUID monitorId) {
        return ResponseEntity.ok(metricsService.getBrands(monitorId));
    }

    @Operation(summary = "Obter série temporal de captação de anúncios para gráfico de linha")
    @GetMapping("/timeline")
    public ResponseEntity<TimelineMetricsResponse> getTimeline(
            @RequestParam(required = false) UUID monitorId,
            @RequestParam(value = "days", required = false, defaultValue = "14") Integer days) {
        return ResponseEntity.ok(metricsService.getTimeline(monitorId, days));
    }
}
