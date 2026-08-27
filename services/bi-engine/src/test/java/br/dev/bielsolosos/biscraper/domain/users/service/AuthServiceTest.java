package br.dev.bielsolosos.biscraper.domain.users.service;

import br.dev.bielsolosos.biscraper.core.utils.SecurityUtils;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.LoginRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.RefreshRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private RefreshRequest refreshRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("biel", "senha123");
        refreshRequest = new RefreshRequest("refresh-token-1");
    }

    @Test
    @DisplayName("Deve realizar login com sucesso e retornar tokens JWT")
    void loginSuccess() {
        when(securityUtils.generateToken(loginRequest.username())).thenReturn("jwt-token");
        when(refreshTokenService.createRefreshToken(loginRequest.username())).thenReturn("refresh-token");

        TokenResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals("refresh-token", response.refreshToken());
        verify(authenticationManager, times(1)).authenticate(argThat(authentication -> {
            if (!(authentication instanceof UsernamePasswordAuthenticationToken token)) {
                return false;
            }
            return loginRequest.username().equals(token.getPrincipal())
                    && loginRequest.password().equals(token.getCredentials());
        }));
        verify(securityUtils, times(1)).generateToken(loginRequest.username());
        verify(refreshTokenService, times(1)).createRefreshToken(loginRequest.username());
    }

    @Test
    @DisplayName("Deve lançar exceção quando as credenciais forem inválidas")
    void loginThrowsAuthenticationException() {
        doThrow(new BadCredentialsException("Credenciais inválidas"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest));
        verify(securityUtils, never()).generateToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    @DisplayName("Deve renovar access token com sucesso usando refresh token válido")
    void refreshSuccess() {
        when(refreshTokenService.validateAndConsume(refreshRequest.refreshToken())).thenReturn("biel");
        when(securityUtils.generateToken("biel")).thenReturn("jwt-token-2");
        when(refreshTokenService.createRefreshToken("biel")).thenReturn("refresh-token-2");

        TokenResponse response = authService.refresh(refreshRequest);

        assertNotNull(response);
        assertEquals("jwt-token-2", response.token());
        assertEquals("refresh-token-2", response.refreshToken());
        verify(refreshTokenService, times(1)).validateAndConsume(refreshRequest.refreshToken());
        verify(securityUtils, times(1)).generateToken("biel");
        verify(refreshTokenService, times(1)).createRefreshToken("biel");
    }

    @Test
    @DisplayName("Deve lançar exceção quando o refresh token for inválido")
    void refreshInvalidToken() {
        when(refreshTokenService.validateAndConsume(refreshRequest.refreshToken()))
                .thenThrow(new IllegalArgumentException("Refresh token inválido ou já utilizado"));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.refresh(refreshRequest)
        );

        assertEquals("Refresh token inválido ou já utilizado", ex.getMessage());
        verify(securityUtils, never()).generateToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }
}
