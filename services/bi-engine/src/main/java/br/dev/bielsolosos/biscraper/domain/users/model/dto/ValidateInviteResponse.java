package br.dev.bielsolosos.biscraper.domain.users.model.dto;

import java.time.Instant;

public record ValidateInviteResponse(
        String email,
        boolean valid,
        Instant expiresAt
) {}
