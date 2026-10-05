package br.dev.bielsolosos.biscraper.domain.users.repository;

import br.dev.bielsolosos.biscraper.core.enums.UserInviteStatus;
import br.dev.bielsolosos.biscraper.domain.users.model.UserInvite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserInviteRepository extends JpaRepository<UserInvite, UUID> {

    Optional<UserInvite> findByToken(String token);

    Optional<UserInvite> findByEmailAndStatus(String email, UserInviteStatus status);

    List<UserInvite> findAllByEmailAndStatus(String email, UserInviteStatus status);

    Page<UserInvite> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
