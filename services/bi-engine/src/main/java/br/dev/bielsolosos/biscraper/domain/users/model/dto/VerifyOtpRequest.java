package br.dev.bielsolosos.biscraper.domain.users.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Payload para validar o código OTP e efetuar o login")
public record VerifyOtpRequest(
        @Schema(description = "Nome de usuário ou e-mail cadastrado na conta", example = "bielsolosos")
        @NotBlank(message = "O identificador (usuário ou e-mail) é obrigatório")
        String identifier,

        @Schema(description = "Código numérico de 6 dígitos recebido por e-mail", example = "481920")
        @NotBlank(message = "O código é obrigatório")
        @Pattern(regexp = "^\\d{6}$", message = "O código deve conter exatamente 6 dígitos numéricos")
        String code
) {}
