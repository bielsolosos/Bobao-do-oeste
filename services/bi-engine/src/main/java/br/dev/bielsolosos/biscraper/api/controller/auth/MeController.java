package br.dev.bielsolosos.biscraper.api.controller.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.dev.bielsolosos.biscraper.core.model.MessageResponse;
import br.dev.bielsolosos.biscraper.domain.users.mapper.UserMapper;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.model.UserConfig;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.AvailableAiModelsResponse;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.ChangePasswordRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.EditUserRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.UserConfigRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.UserConfigResponse;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.UserResponse;
import br.dev.bielsolosos.biscraper.domain.users.service.UserConfigService;
import br.dev.bielsolosos.biscraper.domain.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "User Profile & Config", description = "Endpoints de perfil e configurações do usuário autenticado")
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MeController {

    private final UserConfigService userConfigService;
    private final UserService userService;

    @Operation(summary = "Retorna os dados e configurações do usuário atualmente autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public ResponseEntity<UserResponse> getProfile(Authentication authentication) {
        User user = userService.findUserByUsername(authentication.getName());
        UserConfig config = userConfigService.getConfigForUser(user);
        return ResponseEntity.ok(UserMapper.toUserResponse(user, config));
    }

    @Operation(summary = "Atualiza as configurações de IA do usuário autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/configs")
    public ResponseEntity<UserConfigResponse> updateConfig(
            Authentication authentication,
            @Valid @RequestBody UserConfigRequest request) {
        User user = userService.findUserByUsername(authentication.getName());

        UserConfigResponse response = userConfigService.updateConfigForUser(user, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Retorna o catálogo de provedores e modelos de IA disponíveis", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/configs/models")
    public ResponseEntity<AvailableAiModelsResponse> getAvailableModels() {
        return ResponseEntity.ok(userConfigService.getAvailableModelsCatalog());
    }

    @Operation(summary = "Altera a senha do usuário autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/change-password/{id}")
    public ResponseEntity<MessageResponse> changePassword(Authentication authentication,
            @RequestBody ChangePasswordRequest request) {

        if (!request.oldPassword().equals(request.oldPasswordConfirmation())) {
            return ResponseEntity.badRequest().body(new MessageResponse("As senhas não coincidem."));
        }

        userService.changePassword(userService.findUserByUsername(authentication.getName()),
                request.oldPassword(), request.newPassword());

        return ResponseEntity.ok(new MessageResponse("Senha trocada com sucesso."));
    }

    @Operation(summary = "Atualiza as credenciais (username e email) do usuário autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/edit-credentials")
    public ResponseEntity<UserResponse> editUser(Authentication authentication,
            @Valid @RequestBody EditUserRequest request) {
        return ResponseEntity.ok(
                UserMapper.toUserResponse(
                        userService.editUser(userService.findUserByUsername(authentication.getName()),
                                request.username(), request.email())));
    }
}
