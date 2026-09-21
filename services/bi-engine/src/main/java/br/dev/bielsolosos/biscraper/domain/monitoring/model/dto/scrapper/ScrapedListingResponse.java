package br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper;

import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ScrapedListingResponse(
    UUID id,
    UUID productMonitorId,
    String productMonitorName,
    Vendor vendor,
    String vendorListingId,
    String title,
    String url,
    String description,
    BigDecimal currentPrice,
    BigDecimal originalPrice,
    String state,
    String city,
    String neighborhood,
    boolean hasDelivery,
    String deliveryType,
    List<String> images,
    MatchTier matchTier,
    BigDecimal matchScore,
    Map<String, Object> extractedSpecs,
    OffsetDateTime publishedAt,
    OffsetDateTime firstSeenAt,
    OffsetDateTime lastSeenAt
) {}
