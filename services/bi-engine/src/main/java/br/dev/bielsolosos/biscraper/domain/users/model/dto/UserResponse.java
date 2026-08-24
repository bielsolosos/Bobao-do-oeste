package br.dev.bielsolosos.biscraper.domain.users.model.dto;

import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        boolean active,
        Set<String> roles
) {}
