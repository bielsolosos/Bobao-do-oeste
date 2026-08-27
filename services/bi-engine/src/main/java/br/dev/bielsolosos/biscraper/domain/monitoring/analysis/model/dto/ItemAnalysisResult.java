package br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record ItemAnalysisResult(
    @JsonProperty("vendor_listing_id") String vendorListingId,
    @JsonProperty("score") BigDecimal score,
    @JsonProperty("summary") String summary,
    @JsonProperty("highlights") List<String> highlights,
    @JsonProperty("concerns") List<String> concerns
) {}
