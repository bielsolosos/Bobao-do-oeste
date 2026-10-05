package br.dev.bielsolosos.biscraper.domain.users.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload para alteração de credenciais (username e email) do usuário autenticado")
public record EditUserRequest(
        @Schema(description = "Novo nome de usuário desejado", example = "bielsolosos")
        @NotBlank(message = "O nome de usuário não pode estar em branco")
        String username,

        @Schema(description = "Novo endereço de e-mail desejado", example = "biel@bielsolosos.dev.br")
        @NotBlank(message = "O e-mail não pode estar em branco")
        @Email(message = "Deve ser um email válido")
        String email
) {
}

