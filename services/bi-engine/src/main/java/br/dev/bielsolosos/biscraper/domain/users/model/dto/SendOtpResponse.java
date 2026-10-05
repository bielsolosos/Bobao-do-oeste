package br.dev.bielsolosos.biscraper.domain.users.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta do envio de código OTP de login")
public record SendOtpResponse(
        @Schema(description = "Mensagem informativa", example = "Código enviado para o e-mail cadastrado")
        String message,

        @Schema(description = "Tempo de expiração do código em segundos", example = "600")
        int expiresInSeconds
) {}
