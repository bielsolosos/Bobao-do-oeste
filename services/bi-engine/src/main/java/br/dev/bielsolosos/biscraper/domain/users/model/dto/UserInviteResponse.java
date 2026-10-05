package br.dev.bielsolosos.biscraper.domain.users.model.dto;

import br.dev.bielsolosos.biscraper.core.enums.UserInviteStatus;

import java.time.Instant;
import java.util.UUID;

public record UserInviteResponse(
        UUID id,
        String email,
        String role,
        UserInviteStatus status,
        Instant expiresAt,
        Instant acceptedAt,
        Instant createdAt,
        String invitedByUsername,
        String acceptedByUsername
) {}
