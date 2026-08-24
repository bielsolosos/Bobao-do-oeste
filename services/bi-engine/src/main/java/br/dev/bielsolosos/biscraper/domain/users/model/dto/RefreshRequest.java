package br.dev.bielsolosos.biscraper.domain.users.model.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "Refresh token é obrigatório") String refreshToken
) {}
