package br.dev.bielsolosos.biscraper.domain.monitoring.repository;

import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapedListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScrapedListingRepository extends JpaRepository<ScrapedListing, UUID>, JpaSpecificationExecutor<ScrapedListing> {

    Optional<ScrapedListing> findByProductMonitorIdAndVendorAndVendorListingId(
        UUID productMonitorId,
        Vendor vendor,
        String vendorListingId
    );

    Page<ScrapedListing> findByProductMonitorId(UUID productMonitorId, Pageable pageable);

    Page<ScrapedListing> findByProductMonitorUserId(UUID userId, Pageable pageable);

    Page<ScrapedListing> findByProductMonitorIdAndMatchTier(
        UUID productMonitorId,
        MatchTier matchTier,
        Pageable pageable
    );

    List<ScrapedListing> findByProductMonitorId(UUID productMonitorId);
}
