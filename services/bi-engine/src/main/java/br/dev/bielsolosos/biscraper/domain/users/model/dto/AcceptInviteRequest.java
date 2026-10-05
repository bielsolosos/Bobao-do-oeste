package br.dev.bielsolosos.biscraper.domain.users.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInviteRequest(
        @NotBlank(message = "O token do convite é obrigatório")
        String token,

        @NotBlank(message = "O nome de usuário é obrigatório")
        @Size(min = 3, max = 50, message = "O nome de usuário deve ter entre 3 e 50 caracteres")
        String username,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, max = 100, message = "A senha deve ter no mínimo 6 caracteres")
        String password
) {}
