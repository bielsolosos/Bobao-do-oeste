package br.dev.bielsolosos.biscraper.api.controller.auth;

import br.dev.bielsolosos.biscraper.domain.users.mapper.UserMapper;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.model.UserConfig;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.AvailableAiModelsResponse;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.UserConfigRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.UserConfigResponse;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.UserResponse;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserRepository;
import br.dev.bielsolosos.biscraper.domain.users.service.UserConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Profile & Config", description = "Endpoints de perfil e configurações do usuário autenticado")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepository;
    private final UserConfigService userConfigService;

    @Operation(summary = "Retorna os dados e configurações do usuário atualmente autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        UserConfig config = userConfigService.getConfigForUser(user);
        return ResponseEntity.ok(UserMapper.toUserResponse(user, config));
    }

    @Operation(summary = "Atualiza as configurações de IA do usuário autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/configs")
    public ResponseEntity<UserConfigResponse> updateConfig(
            Authentication authentication,
            @Valid @RequestBody UserConfigRequest request
    ) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        UserConfigResponse response = userConfigService.updateConfigForUser(user, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Retorna o catálogo de provedores e modelos de IA disponíveis", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/configs/models")
    public ResponseEntity<AvailableAiModelsResponse> getAvailableModels() {
        return ResponseEntity.ok(userConfigService.getAvailableModelsCatalog());
    }
}
