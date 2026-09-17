package br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record ItemAnalysisResult(
        @JsonProperty("id")
        @JsonAlias({"vendor_listing_id", "vendorListingId"})
        String vendorListingId,

        @JsonProperty("score")
        BigDecimal score,

        @JsonProperty("reason")
        @JsonAlias({"summary"})
        String summary
) {}
