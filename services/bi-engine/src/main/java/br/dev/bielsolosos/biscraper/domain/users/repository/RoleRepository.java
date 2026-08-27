package br.dev.bielsolosos.biscraper.domain.users.repository;

import br.dev.bielsolosos.biscraper.domain.users.model.Role;
import br.dev.bielsolosos.biscraper.core.enums.RoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleEnum name);
}
