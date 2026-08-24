package br.dev.bielsolosos.biscraper.domain.users.repository;

import br.dev.bielsolosos.biscraper.domain.users.model.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefreshToken r JOIN FETCH r.user WHERE r.token = :token")
    Optional<RefreshToken> findByTokenForUpdate(@Param("token") String token);

    long deleteByExpiresAtBefore(Instant now);
}
