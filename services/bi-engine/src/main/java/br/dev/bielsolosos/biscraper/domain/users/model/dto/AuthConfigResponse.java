package br.dev.bielsolosos.biscraper.domain.users.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Configurações e recursos de autenticação disponíveis na plataforma")
public record AuthConfigResponse(
        @Schema(description = "Indica se o login via código temporário (OTP) por e-mail está habilitado", example = "true")
        boolean emailOtpEnabled
) {}
