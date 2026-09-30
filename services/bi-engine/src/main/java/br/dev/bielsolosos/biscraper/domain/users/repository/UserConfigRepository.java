package br.dev.bielsolosos.biscraper.domain.users.repository;

import br.dev.bielsolosos.biscraper.domain.users.model.UserConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserConfigRepository extends JpaRepository<UserConfig, UUID> {

    @Query("SELECT c FROM UserConfig c JOIN FETCH c.user u")
    List<UserConfig> findAllWithUser();

    Optional<UserConfig> findByUserId(UUID userId);
}

