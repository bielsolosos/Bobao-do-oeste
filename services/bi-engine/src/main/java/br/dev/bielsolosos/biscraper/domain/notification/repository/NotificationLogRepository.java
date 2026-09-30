package br.dev.bielsolosos.biscraper.domain.notification.repository;

import br.dev.bielsolosos.biscraper.domain.notification.model.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    Page<NotificationLog> findByRecipientId(UUID recipientId, Pageable pageable);
}
