package br.dev.bielsolosos.biscraper.domain.users.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload para solicitação de troca de senha do usuário autenticado")
public record ChangePasswordRequest(
        @Schema(description = "Senha atual do usuário", example = "senhaAntiga123")
        @NotBlank(message = "A senha atual não pode estar em branco")
        String oldPassword,

        @Schema(description = "Confirmação da senha atual", example = "senhaAntiga123")
        @NotBlank(message = "A confirmação da senha atual não pode estar em branco")
        String oldPasswordConfirmation,

        @Schema(description = "Nova senha desejada", example = "novaSenhaForte456")
        @NotBlank(message = "A nova senha não pode estar em branco")
        String newPassword
) {
}

