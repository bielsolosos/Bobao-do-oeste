package br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto;

import br.dev.bielsolosos.biscraper.core.enums.DeliveryType;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ScrapeDetailResponse(
        boolean success,
        @JsonProperty("from_cache") boolean fromCache,
        @JsonProperty("used_fallback") boolean usedFallback,
        DetailDto data,
        @JsonProperty("error_message") String errorMessage
) {

    public record DetailDto(
            Vendor vendor,
            @JsonProperty("vendor_listing_id") String vendorListingId,
            String url,
            String title,
            BigDecimal price,
            @JsonProperty("original_price") BigDecimal originalPrice,
            String description,
            String state,
            String city,
            String neighborhood,
            @JsonProperty("has_delivery") Boolean hasDelivery,
            @JsonProperty("delivery_type") DeliveryType deliveryType,
            Map<String, Object> properties,
            List<String> images,
            @JsonProperty("cached_images") List<CachedImageDto> cachedImages,
            @JsonProperty("seller_name") String sellerName,
            @JsonProperty("seller_info") Map<String, Object> sellerInfo,
            @JsonProperty("published_at") Instant publishedAt,
            @JsonProperty("scraped_at") Instant scrapedAt
    ) {}

    public record CachedImageDto(
            String id,
            @JsonProperty("image_index") Integer imageIndex,
            @JsonProperty("original_url") String originalUrl,
            @JsonProperty("endpoint_url") String endpointUrl,
            @JsonProperty("full_endpoint_url") String fullEndpointUrl,
            @JsonProperty("mime_type") String mimeType,
            @JsonProperty("size_bytes") Long sizeBytes,
            @JsonProperty("expires_at") Instant expiresAt
    ) {}
}
