package br.dev.bielsolosos.biscraper.domain.ai.repository;

import br.dev.bielsolosos.biscraper.domain.ai.model.AiAnalysisLog;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class AiAnalysisLogSpecifications {

    private AiAnalysisLogSpecifications() {}

    public static Specification<AiAnalysisLog> filter(UUID ownerUserId, String status, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<AiAnalysisLog, ProductMonitor> monitorJoin = root.join("productMonitor", jakarta.persistence.criteria.JoinType.LEFT);
            predicates.add(cb.equal(monitorJoin.get("user").get("id"), ownerUserId));

            if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(cb.lower(root.get("status")), status.toLowerCase()));
            }

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(monitorJoin.get("name")), like),
                        cb.like(cb.lower(root.get("modelName")), like),
                        cb.like(cb.lower(root.get("vendor")), like)
                ));
            }

            assert query != null;
            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
