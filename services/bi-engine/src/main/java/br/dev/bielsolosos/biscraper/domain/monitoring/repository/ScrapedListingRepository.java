package br.dev.bielsolosos.biscraper.domain.monitoring.repository;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapedListing;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.enums.ListingStatus;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.enums.MatchTier;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.enums.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScrapedListingRepository extends JpaRepository<ScrapedListing, UUID> {

    Optional<ScrapedListing> findByProductMonitorIdAndVendorAndVendorListingId(
        UUID productMonitorId,
        Vendor vendor,
        String vendorListingId
    );

    Page<ScrapedListing> findByProductMonitorId(UUID productMonitorId, Pageable pageable);

    Page<ScrapedListing> findByProductMonitorIdAndMatchTier(
        UUID productMonitorId,
        MatchTier matchTier,
        Pageable pageable
    );

    Page<ScrapedListing> findByProductMonitorIdAndStatus(
        UUID productMonitorId,
        ListingStatus status,
        Pageable pageable
    );

    List<ScrapedListing> findByProductMonitorId(UUID productMonitorId);
}
