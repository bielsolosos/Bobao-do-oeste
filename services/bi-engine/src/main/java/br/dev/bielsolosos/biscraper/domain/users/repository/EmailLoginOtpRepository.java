package br.dev.bielsolosos.biscraper.domain.users.repository;

import br.dev.bielsolosos.biscraper.domain.users.model.EmailLoginOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailLoginOtpRepository extends JpaRepository<EmailLoginOtp, UUID> {

    Optional<EmailLoginOtp> findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(UUID userId);

    long deleteByExpiresAtBefore(Instant now);

    long deleteByUserId(UUID userId);
}
