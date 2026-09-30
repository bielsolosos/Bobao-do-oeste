package br.dev.bielsolosos.biscraper.infrastructure.client.email.dto;

public record EmailSendRequest(
        String to,
        String subject,
        String html,
        String text
) {}
