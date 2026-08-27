package br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto;

import java.math.BigDecimal;

public record ScrapeJobRequest(
    String vendor,
    String keyword,
    String state,
    String region,
    String category,
    BigDecimal min_price,
    BigDecimal max_price,
    boolean require_delivery,
    int max_pages,
    boolean force_browser
) {}
