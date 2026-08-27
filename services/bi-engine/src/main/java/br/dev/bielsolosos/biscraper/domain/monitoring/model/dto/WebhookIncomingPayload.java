package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto;

import br.dev.bielsolosos.biscraper.core.enums.ExecutionStatus;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookIncomingPayload(
    @JsonProperty("requestId")
    @JsonAlias("request_id")
    String requestId,

    @JsonProperty("jobId")
    @JsonAlias("job_id")
    String jobId,

    String status,

    ScrapeResponse response
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScrapeResponse(
        boolean success,
        ExecutionSummary execution,
        List<ScrapedListingItem> items
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExecutionSummary(
        String id,
        Vendor vendor,
        ExecutionStatus status,
        @JsonProperty("duration_ms") Integer durationMs,
        @JsonProperty("total_found") int totalFound,
        @JsonProperty("new_items_count") int newItemsCount,
        @JsonProperty("used_fallback") boolean usedFallback,
        @JsonProperty("error_message") String errorMessage,
        @JsonProperty("started_at") OffsetDateTime startedAt,
        @JsonProperty("finished_at") OffsetDateTime finishedAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScrapedListingItem(
        String id,
        Vendor vendor,
        @JsonProperty("vendor_listing_id") String vendorListingId,
        String title,
        BigDecimal price,
        @JsonProperty("original_price") BigDecimal originalPrice,
        String url,
        String description,
        String state,
        String city,
        String neighborhood,
        @JsonProperty("has_delivery") boolean hasDelivery,
        @JsonProperty("delivery_type") String deliveryType,
        List<String> images,
        @JsonProperty("published_at") OffsetDateTime publishedAt,
        @JsonProperty("scraped_at") OffsetDateTime scrapedAt
    ) {}
}
