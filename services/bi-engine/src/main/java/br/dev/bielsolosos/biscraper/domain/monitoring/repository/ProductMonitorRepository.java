package br.dev.bielsolosos.biscraper.domain.monitoring.repository;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductMonitorRepository extends JpaRepository<ProductMonitor, UUID> {

    List<ProductMonitor> findByUserId(UUID userId);

    Page<ProductMonitor> findByUserId(UUID userId, Pageable pageable);

    Optional<ProductMonitor> findByIdAndUserId(UUID id, UUID userId);

    List<ProductMonitor> findByActiveTrue();
}
