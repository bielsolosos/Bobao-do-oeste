package br.dev.bielsolosos.biscraper.domain.monitoring.repository;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.ListingPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ListingPriceHistoryRepository extends JpaRepository<ListingPriceHistory, Long> {

    List<ListingPriceHistory> findByScrapedListingIdOrderByRecordedAtAsc(UUID listingId);
}
