package br.dev.bielsolosos.biscraper.api.controller.auth;

import br.dev.bielsolosos.biscraper.domain.users.model.dto.AuthConfigResponse;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.LoginRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.RefreshRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.TokenResponse;
import br.dev.bielsolosos.biscraper.domain.users.service.AuthService;
import br.dev.bielsolosos.biscraper.infrastructure.BiScraperProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Endpoints de login tradicional, renovação de tokens JWT e configurações públicas de autenticação")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final BiScraperProperties properties;

    @Operation(summary = "Retorna as funcionalidades de autenticação ativas no ambiente (ex: login por código via e-mail)")
    @GetMapping("/config")
    public ResponseEntity<AuthConfigResponse> getAuthConfig() {
        boolean emailOtpEnabled = properties.getAuth().getOtp().isEnabled() && properties.getEmail().isEnabled();
        return ResponseEntity.ok(new AuthConfigResponse(emailOtpEnabled));
    }

    @Operation(summary = "Realiza o login tradicional com senha e retorna os tokens JWT de acesso e refresh")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Renova o Access Token utilizando um Refresh Token válido")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}
