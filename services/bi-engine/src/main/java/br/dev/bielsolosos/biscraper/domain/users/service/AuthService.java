package br.dev.bielsolosos.biscraper.domain.users.service;

import br.dev.bielsolosos.biscraper.core.utils.SecurityUtils;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.LoginRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.RefreshRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityUtils jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public TokenResponse login(LoginRequest request) throws AuthenticationException {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        String token = jwtUtil.generateToken(request.username());
        String refreshToken = refreshTokenService.createRefreshToken(request.username());

        return new TokenResponse(token, refreshToken);
    }

    public TokenResponse refresh(RefreshRequest request) {
        String username = refreshTokenService.validateAndConsume(request.refreshToken());
        return new TokenResponse(
                jwtUtil.generateToken(username),
                refreshTokenService.createRefreshToken(username)
        );
    }
}
