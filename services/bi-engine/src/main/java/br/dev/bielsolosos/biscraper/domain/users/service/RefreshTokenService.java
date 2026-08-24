package br.dev.bielsolosos.biscraper.domain.users.service;

import br.dev.bielsolosos.biscraper.domain.users.model.RefreshToken;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.repository.RefreshTokenRepository;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserRepository;
import br.dev.bielsolosos.biscraper.infrastructure.BiScraperProperties;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final BiScraperProperties properties;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public String createRefreshToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusMillis(properties.getJwt().getRefreshExpiration());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public String validateAndConsume(String token) {
        RefreshToken data = refreshTokenRepository.findByTokenForUpdate(token)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token inválido ou já utilizado"));

        refreshTokenRepository.delete(data);

        if (data.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expirado");
        }

        return data.getUser().getUsername();
    }

    @Transactional
    public void cleanupExpiredTokens() {
        long removed = refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
        if (removed > 0) {
            log.debug("Removidos {} refresh tokens expirados", removed);
        }
    }
}
