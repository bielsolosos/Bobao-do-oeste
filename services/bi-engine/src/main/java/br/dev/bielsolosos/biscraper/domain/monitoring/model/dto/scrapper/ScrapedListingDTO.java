package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper;

import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScrapedListingDTO(
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
