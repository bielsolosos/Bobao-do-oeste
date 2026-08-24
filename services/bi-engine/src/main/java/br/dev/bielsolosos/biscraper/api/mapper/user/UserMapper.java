package br.dev.bielsolosos.biscraper.api.mapper.user;

import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.UserResponse;

import java.util.stream.Collectors;

public class UserMapper {

    private UserMapper() {}

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isActive(),
                user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet())
        );
    }
}
