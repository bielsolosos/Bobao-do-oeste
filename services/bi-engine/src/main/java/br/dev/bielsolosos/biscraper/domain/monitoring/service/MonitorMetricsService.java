package br.dev.bielsolosos.biscraper.domain.monitoring.service;

import br.dev.bielsolosos.biscraper.core.exception.BusinessException;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics.*;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ProductMonitorRepository;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ScrapedListingRepository;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.service.MeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorMetricsService {

    private final ScrapedListingRepository scrapedListingRepository;
    private final ProductMonitorRepository productMonitorRepository;
    private final MeService meService;

    @Transactional(readOnly = true)
    public MetricsOverviewResponse getOverview(UUID monitorId) {
        User me = meService.getMe();
        OffsetDateTime lastScrapedAt;
        OverviewStatsDto stats;

        if (monitorId != null) {
            ProductMonitor monitor = getAndValidateMonitor(monitorId, me);
            lastScrapedAt = monitor.getLastScrapedAt();
            stats = scrapedListingRepository.getOverviewStatsByMonitorId(monitorId);
        } else {
            stats = scrapedListingRepository.getOverviewStatsByUserId(me.getId());
            lastScrapedAt = productMonitorRepository.findByUserId(me.getId()).stream()
                    .map(ProductMonitor::getLastScrapedAt)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
        }

        return stats != null
                ? stats.toResponse(lastScrapedAt)
                : new OverviewStatsDto(0, 0, null, null, null).toResponse(lastScrapedAt);
    }

    @Transactional(readOnly = true)
    public TierMetricsResponse getTiers(UUID monitorId) {
        User me = meService.getMe();
        if (monitorId != null) {
            getAndValidateMonitor(monitorId, me);
            return scrapedListingRepository.getTierMetricsByMonitorId(monitorId);
        }
        return scrapedListingRepository.getTierMetricsByUserId(me.getId());
    }

    @Transactional(readOnly = true)
    public PriceDistributionResponse getPrices(UUID monitorId) {
        User me = meService.getMe();
        PriceBucketCountsDto counts;

        if (monitorId != null) {
            ProductMonitor monitor = getAndValidateMonitor(monitorId, me);
            if (monitor.getAnalysisType() == br.dev.bielsolosos.biscraper.core.enums.AnalysisType.NONE) {
                counts = scrapedListingRepository.getPriceBucketsByMonitorIdAll(monitorId);
            } else {
                counts = scrapedListingRepository.getPriceBucketsByMonitorId(monitorId);
                if (counts == null || counts.total() == 0) {
                    counts = scrapedListingRepository.getPriceBucketsByMonitorIdAll(monitorId);
                }
            }
        } else {
            counts = scrapedListingRepository.getPriceBucketsByUserId(me.getId());
            if (counts == null || counts.total() == 0) {
                counts = scrapedListingRepository.getPriceBucketsByUserIdAll(me.getId());
            }
        }

        return (counts != null) ? counts.toResponse() : PriceBucketCountsDto.empty().toResponse();
    }

    @Transactional(readOnly = true)
    public BrandDistributionResponse getBrands(UUID monitorId) {
        User me = meService.getMe();
        List<br.dev.bielsolosos.biscraper.domain.monitoring.repository.projection.SpecsAndTitleProjection> rows;

        if (monitorId != null) {
            ProductMonitor monitor = getAndValidateMonitor(monitorId, me);
            if (monitor.getAnalysisType() == br.dev.bielsolosos.biscraper.core.enums.AnalysisType.NONE) {
                rows = scrapedListingRepository.findSpecsAndTitleByMonitorIdAll(monitorId);
            } else {
                rows = scrapedListingRepository.findSpecsAndTitleByMonitorId(monitorId);
                if (rows.isEmpty()) {
                    rows = scrapedListingRepository.findSpecsAndTitleByMonitorIdAll(monitorId);
                }
            }
        } else {
            rows = scrapedListingRepository.findSpecsAndTitleByUserId(me.getId());
            if (rows.isEmpty()) {
                rows = scrapedListingRepository.findSpecsAndTitleByUserIdAll(me.getId());
            }
        }

        Map<String, Long> brandCounts = new HashMap<>();

        for (br.dev.bielsolosos.biscraper.domain.monitoring.repository.projection.SpecsAndTitleProjection row : rows) {
            Map<String, Object> specs = row.getExtractedSpecs();
            String brand = (specs != null && specs.get("brand") != null)
                    ? specs.get("brand").toString().toUpperCase().trim()
                    : null;

            if (brand == null || brand.isBlank() || "OTHER".equalsIgnoreCase(brand) || "OUTRA".equalsIgnoreCase(brand)) {
                brand = inferBrandFromTitle(row.getTitle());
            }

            String key = (brand != null && !brand.isBlank()) ? brand : "OUTRAS";
            brandCounts.put(key, brandCounts.getOrDefault(key, 0L) + 1);
        }

        List<BrandDistributionResponse.BrandItem> items = brandCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(e -> new BrandDistributionResponse.BrandItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return new BrandDistributionResponse(items);
    }

    @Transactional(readOnly = true)
    public TimelineMetricsResponse getTimeline(UUID monitorId, Integer days) {
        User me = meService.getMe();
        int safeDays = (days != null && days > 0 && days <= 90) ? days : 14;
        OffsetDateTime startDate = OffsetDateTime.now().minusDays(safeDays);

        List<br.dev.bielsolosos.biscraper.domain.monitoring.repository.projection.TimelineCountProjection> rows;
        if (monitorId != null) {
            ProductMonitor monitor = getAndValidateMonitor(monitorId, me);
            if (monitor.getAnalysisType() == br.dev.bielsolosos.biscraper.core.enums.AnalysisType.NONE) {
                rows = scrapedListingRepository.countTimelineByMonitorIdAll(monitorId, startDate);
            } else {
                rows = scrapedListingRepository.countTimelineByMonitorId(monitorId, startDate);
                if (rows.isEmpty()) {
                    rows = scrapedListingRepository.countTimelineByMonitorIdAll(monitorId, startDate);
                }
            }
        } else {
            rows = scrapedListingRepository.countTimelineByUserId(me.getId(), startDate);
            if (rows.isEmpty()) {
                rows = scrapedListingRepository.countTimelineByUserIdAll(me.getId(), startDate);
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, Long> countsByDate = rows.stream()
                .filter(r -> r.getDate() != null)
                .collect(Collectors.toMap(
                        r -> (r.getDate() instanceof java.sql.Date d) ? d.toLocalDate().format(formatter)
                           : (r.getDate() instanceof LocalDate d) ? d.format(formatter)
                           : r.getDate().toString(),
                        br.dev.bielsolosos.biscraper.domain.monitoring.repository.projection.TimelineCountProjection::getCount,
                        Long::sum
                ));

        List<TimelineMetricsResponse.TimelinePoint> points = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = safeDays - 1; i >= 0; i--) {
            String dateStr = today.minusDays(i).format(formatter);
            points.add(new TimelineMetricsResponse.TimelinePoint(dateStr, countsByDate.getOrDefault(dateStr, 0L)));
        }

        return new TimelineMetricsResponse(points);
    }

    private ProductMonitor getAndValidateMonitor(UUID monitorId, User me) {
        ProductMonitor monitor = productMonitorRepository.findById(monitorId)
                .orElseThrow(() -> new BusinessException("Monitor de produto não encontrado com o ID fornecido."));
        if (!monitor.getUser().getId().equals(me.getId())) {
            log.warn("Acesso negado: Usuário '{}' tentou acessar métricas do monitor '{}' de outro usuário.", me.getUsername(), monitorId);
            throw new BusinessException("Você não tem permissão para acessar os dados deste monitor.");
        }
        return monitor;
    }

    private String inferBrandFromTitle(String title) {
        if (title == null) return null;
        String lower = title.toLowerCase();
        if (lower.contains("macbook") || lower.contains("apple")) return "APPLE";
        if (lower.contains("dell") || lower.contains("alienware")) return "DELL";
        if (lower.contains("lenovo") || lower.contains("thinkpad") || lower.contains("ideapad") || lower.contains("legion")) return "LENOVO";
        if (lower.contains("acer") || lower.contains("nitro") || lower.contains("predator") || lower.contains("aspire")) return "ACER";
        if (lower.contains("asus") || lower.contains("rog") || lower.contains("tuf") || lower.contains("zenbook")) return "ASUS";
        if (lower.contains("hp") || lower.contains("pavilion") || lower.contains("omen") || lower.contains("victus")) return "HP";
        if (lower.contains("samsung") || lower.contains("galaxy book")) return "SAMSUNG";
        if (lower.contains("avell")) return "AVELL";
        if (lower.contains("vaio")) return "VAIO";
        if (lower.contains("positiv") || lower.contains("positivo")) return "POSITIVO";
        return null;
    }
}
