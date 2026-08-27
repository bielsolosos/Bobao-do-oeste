package br.dev.bielsolosos.biscraper.domain.users.service;

import br.dev.bielsolosos.biscraper.domain.users.model.RefreshToken;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.repository.RefreshTokenRepository;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserRepository;
import br.dev.bielsolosos.biscraper.infrastructure.BiScraperProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private BiScraperProperties properties;

    @Mock
    private BiScraperProperties.Jwt jwtProperties;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Deve criar refresh token com sucesso")
    void createRefreshTokenSuccess() {
        when(properties.getJwt()).thenReturn(jwtProperties);
        when(jwtProperties.getRefreshExpiration()).thenReturn(60_000L);

        User user = new User();
        user.setUsername("biel");
        when(userRepository.findByUsername("biel")).thenReturn(Optional.of(user));

        String token = refreshTokenService.createRefreshToken("biel");

        assertNotNull(token);
        assertFalse(token.isBlank());

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken savedToken = tokenCaptor.getValue();
        assertEquals("biel", savedToken.getUser().getUsername());
        assertEquals(token, savedToken.getToken());
        assertTrue(savedToken.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar refresh token para usuário inexistente")
    void createRefreshTokenUserNotFound() {
        when(userRepository.findByUsername("inexistente")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> refreshTokenService.createRefreshToken("inexistente"));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Deve validar e consumir refresh token válido")
    void validateAndConsumeSuccess() {
        String token = "refresh-token";
        User user = new User();
        user.setUsername("biel");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findByTokenForUpdate(token)).thenReturn(Optional.of(refreshToken));

        String username = refreshTokenService.validateAndConsume(token);

        assertEquals("biel", username);
        verify(refreshTokenRepository).delete(refreshToken);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar consumir token inexistente ou já utilizado")
    void validateAndConsumeAlreadyConsumedToken() {
        String token = "already-consumed";
        when(refreshTokenRepository.findByTokenForUpdate(token)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> refreshTokenService.validateAndConsume(token)
        );

        assertEquals("Refresh token inválido ou já utilizado", ex.getMessage());
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar consumir token expirado")
    void validateAndConsumeExpiredToken() {
        String token = "expired-token";
        User user = new User();
        user.setUsername("biel");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().minusSeconds(1));
        when(refreshTokenRepository.findByTokenForUpdate(token)).thenReturn(Optional.of(refreshToken));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> refreshTokenService.validateAndConsume(token)
        );

        assertEquals("Refresh token expirado", ex.getMessage());
        verify(refreshTokenRepository).delete(refreshToken);
    }

    @Test
    @DisplayName("Deve limpar tokens expirados com sucesso")
    void cleanupExpiredTokensRemovesExpiredToken() {
        when(refreshTokenRepository.deleteByExpiresAtBefore(any(Instant.class))).thenReturn(1L);

        refreshTokenService.cleanupExpiredTokens();

        verify(refreshTokenRepository).deleteByExpiresAtBefore(any(Instant.class));
    }
}
