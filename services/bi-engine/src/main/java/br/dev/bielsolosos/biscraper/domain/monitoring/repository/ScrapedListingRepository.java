package br.dev.bielsolosos.biscraper.domain.monitoring.repository;

import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapedListing;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics.OverviewStatsDto;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics.PriceBucketCountsDto;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics.TierMetricsResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.projection.SpecsAndTitleProjection;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.projection.TimelineCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScrapedListingRepository
        extends JpaRepository<ScrapedListing, UUID>, JpaSpecificationExecutor<ScrapedListing> {

    Optional<ScrapedListing> findByProductMonitorIdAndVendorAndVendorListingId(
            UUID productMonitorId,
            Vendor vendor,
            String vendorListingId);

    Page<ScrapedListing> findByProductMonitorId(UUID productMonitorId, Pageable pageable);

    Page<ScrapedListing> findByProductMonitorUserId(UUID userId, Pageable pageable);

    Page<ScrapedListing> findByProductMonitorIdAndMatchTier(
            UUID productMonitorId,
            MatchTier matchTier,
            Pageable pageable);

    List<ScrapedListing> findByProductMonitorId(UUID productMonitorId);

    @Query("SELECT new br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics.OverviewStatsDto(" +
            "COUNT(s), " +
            "COUNT(CASE WHEN s.matchTier = br.dev.bielsolosos.biscraper.core.enums.MatchTier.HIGH THEN 1 END), " +
            "MIN(s.currentPrice), " +
            "MAX(s.currentPrice), " +
            "AVG(s.currentPrice)) " +
            "FROM ScrapedListing s WHERE s.productMonitor.user.id = :userId")
    OverviewStatsDto getOverviewStatsByUserId(@Param("userId") UUID userId);

    @Query("SELECT new br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics.OverviewStatsDto(" +
            "COUNT(s), " +
            "COUNT(CASE WHEN s.matchTier = br.dev.bielsolosos.biscraper.core.enums.MatchTier.HIGH THEN 1 END), " +
            "MIN(s.currentPrice), " +
            "MAX(s.currentPrice), " +
            "AVG(s.currentPrice)) " +
            "FROM ScrapedListing s WHERE s.productMonitor.id = :productMonitorId")
    OverviewStatsDto getOverviewStatsByMonitorId(@Param("productMonitorId") UUID productMonitorId);

    @Query("SELECT new br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics.TierMetricsResponse(" +
            "COUNT(CASE WHEN s.matchTier = br.dev.bielsolosos.biscraper.core.enums.MatchTier.HIGH THEN 1 END), " +
            "COUNT(CASE WHEN s.matchTier = br.dev.bielsolosos.biscraper.core.enums.MatchTier.MEDIUM THEN 1 END), " +
            "COUNT(CASE WHEN s.matchTier = br.dev.bielsolosos.biscraper.core.enums.MatchTier.LOW THEN 1 END), " +
            "COUNT(CASE WHEN s.matchTier = br.dev.bielsolosos.biscraper.core.enums.MatchTier.NONE THEN 1 END), " +
            "COUNT(s)) " +
            "FROM ScrapedListing s WHERE s.productMonitor.user.id = :userId")
    TierMetricsResponse getTierMetricsByUserId(@Param("userId") UUID userId);

    @Query("SELECT new br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics.TierMetricsResponse(" +
            "COUNT(CASE WHEN s.matchTier = br.dev.bielsolosos.biscraper.core.enums.MatchTier.HIGH THEN 1 END), " +
            "COUNT(CASE WHEN s.matchTier = br.dev.bielsolosos.biscraper.core.enums.MatchTier.MEDIUM THEN 1 END), " +
            "COUNT(CASE WHEN s.matchTier = br.dev.bielsolosos.biscraper.core.enums.MatchTier.LOW THEN 1 END), " +
            "COUNT(CASE WHEN s.matchTier = br.dev.bielsolosos.biscraper.core.enums.MatchTier.NONE THEN 1 END), " +
            "COUNT(s)) " +
            "FROM ScrapedListing s WHERE s.productMonitor.id = :productMonitorId")
    TierMetricsResponse getTierMetricsByMonitorId(@Param("productMonitorId") UUID productMonitorId);

    @Query("SELECT new br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics.PriceBucketCountsDto(" +
            "COUNT(CASE WHEN s.currentPrice <= 1500 THEN 1 END), " +
            "COUNT(CASE WHEN s.currentPrice > 1500 AND s.currentPrice <= 3000 THEN 1 END), " +
            "COUNT(CASE WHEN s.currentPrice > 3000 AND s.currentPrice <= 5000 THEN 1 END), " +
            "COUNT(CASE WHEN s.currentPrice > 5000 AND s.currentPrice <= 8000 THEN 1 END), " +
            "COUNT(CASE WHEN s.currentPrice > 8000 THEN 1 END)) " +
            "FROM ScrapedListing s WHERE s.productMonitor.user.id = :userId")
    PriceBucketCountsDto getPriceBucketsByUserId(@Param("userId") UUID userId);

    @Query("SELECT new br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.metrics.PriceBucketCountsDto(" +
            "COUNT(CASE WHEN s.currentPrice <= 1500 THEN 1 END), " +
            "COUNT(CASE WHEN s.currentPrice > 1500 AND s.currentPrice <= 3000 THEN 1 END), " +
            "COUNT(CASE WHEN s.currentPrice > 3000 AND s.currentPrice <= 5000 THEN 1 END), " +
            "COUNT(CASE WHEN s.currentPrice > 5000 AND s.currentPrice <= 8000 THEN 1 END), " +
            "COUNT(CASE WHEN s.currentPrice > 8000 THEN 1 END)) " +
            "FROM ScrapedListing s WHERE s.productMonitor.id = :productMonitorId")
    PriceBucketCountsDto getPriceBucketsByMonitorId(@Param("productMonitorId") UUID productMonitorId);

    @Query("SELECT s.extractedSpecs AS extractedSpecs, s.title AS title FROM ScrapedListing s WHERE s.productMonitor.user.id = :userId")
    List<SpecsAndTitleProjection> findSpecsAndTitleByUserId(@Param("userId") UUID userId);

    @Query("SELECT s.extractedSpecs AS extractedSpecs, s.title AS title FROM ScrapedListing s WHERE s.productMonitor.id = :productMonitorId")
    List<SpecsAndTitleProjection> findSpecsAndTitleByMonitorId(@Param("productMonitorId") UUID productMonitorId);

    @Query("SELECT CAST(s.firstSeenAt AS date) AS date, COUNT(s) AS count FROM ScrapedListing s WHERE s.productMonitor.user.id = :userId AND s.firstSeenAt >= :startDate GROUP BY CAST(s.firstSeenAt AS date) ORDER BY CAST(s.firstSeenAt AS date) ASC")
    List<TimelineCountProjection> countTimelineByUserId(@Param("userId") UUID userId,
            @Param("startDate") OffsetDateTime startDate);

    @Query("SELECT CAST(s.firstSeenAt AS date) AS date, COUNT(s) AS count FROM ScrapedListing s WHERE s.productMonitor.id = :productMonitorId AND s.firstSeenAt >= :startDate GROUP BY CAST(s.firstSeenAt AS date) ORDER BY CAST(s.firstSeenAt AS date) ASC")
    List<TimelineCountProjection> countTimelineByMonitorId(@Param("productMonitorId") UUID productMonitorId,
            @Param("startDate") OffsetDateTime startDate);
}
