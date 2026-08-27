package br.dev.bielsolosos.biscraper.domain.monitoring.repository;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.MonitorSearchQuery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MonitorSearchQueryRepository extends JpaRepository<MonitorSearchQuery, UUID> {

    List<MonitorSearchQuery> findByProductMonitorId(UUID productMonitorId);

    List<MonitorSearchQuery> findByProductMonitorIdAndActiveTrue(UUID productMonitorId);
}
