package br.dev.bielsolosos.biscraper.domain.monitoring.repository;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapingExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScrapingExecutionRepository extends JpaRepository<ScrapingExecution, UUID> {

    List<ScrapingExecution> findByProductMonitorId(UUID productMonitorId);

    Page<ScrapingExecution> findByProductMonitorId(UUID productMonitorId, Pageable pageable);

    Optional<ScrapingExecution> findByWebhookEventRequestId(String requestId);
}
