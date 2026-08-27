package br.dev.bielsolosos.biscraper.api.model.productmonitor;

import java.math.BigDecimal;
import java.util.UUID;

public record MonitorSearchQueryResponse(
    UUID id,
    String queryTerm,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    String stateFilter,
    String regionFilter,
    boolean requireDelivery,
    int maxPages,
    boolean active
) {}
