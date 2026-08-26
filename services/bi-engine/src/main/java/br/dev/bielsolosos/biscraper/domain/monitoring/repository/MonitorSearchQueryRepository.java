package br.dev.bielsolosos.biscraper.domain.monitoring.repository;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.MonitorSearchQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MonitorSearchQueryRepository extends JpaRepository<MonitorSearchQuery, UUID> {

    List<MonitorSearchQuery> findByProductMonitorId(UUID productMonitorId);

    List<MonitorSearchQuery> findByProductMonitorIdAndActiveTrue(UUID productMonitorId);
}
