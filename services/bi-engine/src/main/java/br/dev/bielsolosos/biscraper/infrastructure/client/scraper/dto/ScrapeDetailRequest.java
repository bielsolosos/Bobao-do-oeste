package br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import br.dev.bielsolosos.biscraper.core.enums.Vendor;

public record ScrapeDetailRequest(
        String url,
        Vendor vendor,
        @JsonProperty("download_images") boolean downloadImages,
        @JsonProperty("ttl_hours") int ttlHours,
        @JsonProperty("force_browser") boolean forceBrowser
) {
    
}
