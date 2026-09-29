package br.dev.bielsolosos.biscraper.domain.ai.repository;

import br.dev.bielsolosos.biscraper.domain.ai.model.AiAnalysisLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;

import java.util.UUID;

public interface AiAnalysisLogRepository extends JpaRepository<AiAnalysisLog, UUID>, JpaSpecificationExecutor<AiAnalysisLog> {

    @NonNull
    @Override
    @EntityGraph(attributePaths = {"productMonitor", "scrapingExecution"})
    Page<AiAnalysisLog> findAll(Specification<AiAnalysisLog> spec, @NonNull Pageable pageable);

    @EntityGraph(attributePaths = {"productMonitor", "scrapingExecution"})
    Page<AiAnalysisLog> findByProductMonitorUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"productMonitor", "scrapingExecution"})
    Page<AiAnalysisLog> findByProductMonitorId(UUID productMonitorId, Pageable pageable);

    @EntityGraph(attributePaths = {"productMonitor", "scrapingExecution"})
    Page<AiAnalysisLog> findByScrapingExecutionId(UUID scrapingExecutionId, Pageable pageable);
}
