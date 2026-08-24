package br.dev.bielsolosos.biscraper.api.controller;

import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.UserResponse;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@Tag(name = "User Profile", description = "Endpoints de perfil do usuário autenticado")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepository;

    @Operation(summary = "Retorna os dados do usuário atualmente autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        UserResponse response = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isActive(),
                user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet())
        );

        return ResponseEntity.ok(response);
    }
}
