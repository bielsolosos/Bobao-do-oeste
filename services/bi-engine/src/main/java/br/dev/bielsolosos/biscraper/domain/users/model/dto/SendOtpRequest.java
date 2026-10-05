package br.dev.bielsolosos.biscraper.domain.users.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payload para solicitar envio de código OTP de login por e-mail")
public record SendOtpRequest(
        @Schema(description = "Nome de usuário ou e-mail cadastrado na conta", example = "bielsolosos")
        @NotBlank(message = "O identificador (usuário ou e-mail) é obrigatório")
        String identifier
) {}
