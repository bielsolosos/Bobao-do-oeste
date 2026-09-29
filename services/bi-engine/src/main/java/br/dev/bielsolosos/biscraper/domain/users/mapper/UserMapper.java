package br.dev.bielsolosos.biscraper.domain.users.mapper;

import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.model.UserConfig;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.UserResponse;

import java.util.stream.Collectors;

public class UserMapper {

    private UserMapper() {}

    public static UserResponse toUserResponse(User user) {
        return toUserResponse(user, null);
    }

    public static UserResponse toUserResponse(User user, UserConfig config) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isActive(),
                user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()),
                UserConfigMapper.toResponse(config)
        );
    }
}
