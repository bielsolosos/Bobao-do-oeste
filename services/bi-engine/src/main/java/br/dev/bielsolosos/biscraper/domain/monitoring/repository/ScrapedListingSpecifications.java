package br.dev.bielsolosos.biscraper.domain.monitoring.repository;

import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapedListing;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ScrapedListingSpecifications {

    private ScrapedListingSpecifications() {}

    public static Specification<ScrapedListing> filter(UUID monitorId, String keyword, MatchTier tier, Boolean deliveryOnly) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("productMonitor").get("id"), monitorId));

            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + keyword.trim().toLowerCase() + "%"));
            }

            if (tier != null) {
                predicates.add(cb.equal(root.get("matchTier"), tier));
            }

            if (Boolean.TRUE.equals(deliveryOnly)) {
                predicates.add(cb.isTrue(root.get("hasDelivery")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
