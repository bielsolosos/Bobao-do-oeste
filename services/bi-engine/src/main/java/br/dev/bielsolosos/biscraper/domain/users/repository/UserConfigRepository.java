package br.dev.bielsolosos.biscraper.domain.users.repository;

import br.dev.bielsolosos.biscraper.domain.users.model.UserConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserConfigRepository extends JpaRepository<UserConfig, UUID> {

    Optional<UserConfig> findByUserId(UUID userId);
}
