package br.dev.bielsolosos.biscraper.domain.monitoring.repository;



import br.dev.bielsolosos.biscraper.domain.monitoring.model.WebhookEvent;
import br.dev.bielsolosos.biscraper.core.enums.WebhookStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID>, JpaSpecificationExecutor<WebhookEvent> {

    Optional<WebhookEvent> findByRequestId(String requestId);

    boolean existsByRequestId(String requestId);

    List<WebhookEvent> findByStatus(WebhookStatus status);
}
