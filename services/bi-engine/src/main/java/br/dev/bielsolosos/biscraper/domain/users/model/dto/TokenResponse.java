package br.dev.bielsolosos.biscraper.domain.users.model.dto;

public record TokenResponse(
        String token,
        String refreshToken
) {}
