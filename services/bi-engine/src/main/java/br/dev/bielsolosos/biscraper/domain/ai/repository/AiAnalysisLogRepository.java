package br.dev.bielsolosos.biscraper.domain.ai.repository;

import br.dev.bielsolosos.biscraper.domain.ai.model.AiAnalysisLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiAnalysisLogRepository extends JpaRepository<AiAnalysisLog, UUID> {

    Page<AiAnalysisLog> findByProductMonitorUserId(UUID userId, Pageable pageable);

    Page<AiAnalysisLog> findByProductMonitorId(UUID productMonitorId, Pageable pageable);

    Page<AiAnalysisLog> findByScrapingExecutionId(UUID scrapingExecutionId, Pageable pageable);
}
