package br.dev.bielsolosos.biscraper.api.controller.auth;

import br.dev.bielsolosos.biscraper.domain.users.model.dto.AcceptInviteRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.TokenResponse;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.ValidateInviteResponse;
import br.dev.bielsolosos.biscraper.domain.users.service.UserInviteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Public User Invites", description = "Endpoints públicos para validação e conclusão de cadastro via convite")
@RestController
@RequestMapping("/api/v1/auth/invites")
@RequiredArgsConstructor
public class UserInvitePublicController {

    private final UserInviteService userInviteService;

    @GetMapping("/validate")
    @Operation(summary = "Valida o token do convite e retorna o e-mail associado")
    public ResponseEntity<ValidateInviteResponse> validateInvite(@RequestParam String token) {
        return ResponseEntity.ok(userInviteService.validateInvite(token));
    }

    @PostMapping("/accept")
    @Operation(summary = "Aceita o convite, cria a conta do usuário e efetua o login automático")
    public ResponseEntity<TokenResponse> acceptInvite(@Valid @RequestBody AcceptInviteRequest request) {
        return ResponseEntity.ok(userInviteService.acceptInvite(request));
    }
}
