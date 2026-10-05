package br.dev.bielsolosos.biscraper.core.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta textual padrão para operações da API.
 */
@Schema(description = "Objeto de resposta com mensagem textual informativa")
public record MessageResponse(
        @Schema(description = "Mensagem descritiva do resultado da operação", example = "Operação realizada com sucesso.")
        String Message
) {
}

