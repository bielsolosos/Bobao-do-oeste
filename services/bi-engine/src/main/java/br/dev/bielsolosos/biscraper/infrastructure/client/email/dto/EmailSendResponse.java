package br.dev.bielsolosos.biscraper.infrastructure.client.email.dto;

public record EmailSendResponse(
        boolean success,
        String messageId,
        String error
) {}
