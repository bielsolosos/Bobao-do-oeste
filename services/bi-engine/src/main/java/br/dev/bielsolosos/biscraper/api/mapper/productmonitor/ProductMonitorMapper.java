package br.dev.bielsolosos.biscraper.api.mapper.productmonitor;

import br.dev.bielsolosos.biscraper.api.model.productmonitor.MonitorSearchQueryResponse;
import br.dev.bielsolosos.biscraper.api.model.productmonitor.ProductMonitorRequest;
import br.dev.bielsolosos.biscraper.api.model.productmonitor.ProductMonitorResponse;
import br.dev.bielsolosos.biscraper.core.enums.ScrapingFrequency;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.MonitorSearchQuery;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductMonitorMapper {

    private final ObjectMapper objectMapper;

    public ProductMonitor toEntity(ProductMonitorRequest request, User user) {
        if (request == null) {
            return null;
        }

        JsonNode expectedSpecsNode = null;
        if (request.analysisTypeFields() != null) {
            expectedSpecsNode = objectMapper.valueToTree(request.analysisTypeFields());
        }

        String cron = request.frequency() != null ? request.frequency().getCronExpression() : null;

        ProductMonitor monitor = ProductMonitor.builder()
                .user(user)
                .name(request.name())
                .description(request.description())
                .analysisType(request.analysisType())
                .targetVendor(request.vendor() != null ? request.vendor() : br.dev.bielsolosos.biscraper.core.enums.Vendor.OLX)
                .active(true)
                .cronExpression(cron)
                .expectedSpecs(expectedSpecsNode)
                .searchQueries(new ArrayList<>())
                .listings(new ArrayList<>())
                .build();

        if (request.searchKeywords() != null && !request.searchKeywords().isEmpty()) {
            for (String keyword : request.searchKeywords()) {
                MonitorSearchQuery query = MonitorSearchQuery.builder()
                        .queryTerm(keyword)
                        .minPrice(request.minPrice())
                        .maxPrice(request.maxPrice())
                        .stateFilter(request.stateFilter())
                        .regionFilter(request.regionFilter())
                        .requireDelivery(Boolean.TRUE.equals(request.requireDelivery()))
                        .maxPages(1)
                        .active(true)
                        .build();
                monitor.addSearchQuery(query);
            }
        }

        return monitor;
    }

    public void updateEntity(ProductMonitor monitor, ProductMonitorRequest request) {
        if (monitor == null || request == null) {
            return;
        }

        monitor.setName(request.name());
        monitor.setDescription(request.description());
        if (request.vendor() != null) {
            monitor.setTargetVendor(request.vendor());
        }
        if (request.analysisType() != null) {
            monitor.setAnalysisType(request.analysisType());
        }
        if (request.frequency() != null) {
            monitor.setCronExpression(request.frequency().getCronExpression());
        }

        if (request.analysisTypeFields() != null) {
            monitor.setExpectedSpecs(objectMapper.valueToTree(request.analysisTypeFields()));
        }

        if (request.searchKeywords() != null && !request.searchKeywords().isEmpty()) {
            monitor.clearSearchQueries();
            for (String keyword : request.searchKeywords()) {
                MonitorSearchQuery query = MonitorSearchQuery.builder()
                        .queryTerm(keyword)
                        .minPrice(request.minPrice())
                        .maxPrice(request.maxPrice())
                        .stateFilter(request.stateFilter())
                        .regionFilter(request.regionFilter())
                        .requireDelivery(Boolean.TRUE.equals(request.requireDelivery()))
                        .maxPages(1)
                        .active(true)
                        .build();
                monitor.addSearchQuery(query);
            }
        }
    }

    public ProductMonitorResponse toResponse(ProductMonitor entity) {
        if (entity == null) {
            return null;
        }

        List<MonitorSearchQueryResponse> queryResponses = entity.getSearchQueries() == null
                ? Collections.emptyList()
                : entity.getSearchQueries().stream()
                .map(this::toSearchQueryResponse)
                .toList();

        return new ProductMonitorResponse(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getAnalysisType(),
                entity.getTargetVendor(),
                entity.isActive(),
                ScrapingFrequency.fromCronExpression(entity.getCronExpression()),
                entity.getCronExpression(),
                entity.getExpectedSpecs(),
                queryResponses,
                entity.getLastScrapedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public MonitorSearchQueryResponse toSearchQueryResponse(MonitorSearchQuery query) {
        if (query == null) {
            return null;
        }
        return new MonitorSearchQueryResponse(
                query.getId(),
                query.getQueryTerm(),
                query.getMinPrice(),
                query.getMaxPrice(),
                query.getStateFilter(),
                query.getRegionFilter(),
                query.isRequireDelivery(),
                query.getMaxPages(),
                query.isActive()
        );
    }
}
