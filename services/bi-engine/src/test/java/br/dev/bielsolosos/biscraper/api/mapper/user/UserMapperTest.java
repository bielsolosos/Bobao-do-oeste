package br.dev.bielsolosos.biscraper.api.mapper.user;

import br.dev.bielsolosos.biscraper.core.enums.RoleEnum;
import br.dev.bielsolosos.biscraper.domain.users.model.Role;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.UserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    @Test
    @DisplayName("Deve converter entidade User para UserResponse")
    void shouldMapUserToUserResponse() {
        Role role = new Role();
        role.setId(1L);
        role.setName(RoleEnum.ROLE_USER);

        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .username("bielsolosos")
                .email("biel@dev.com")
                .active(true)
                .roles(Set.of(role))
                .build();

        UserResponse response = UserMapper.toUserResponse(user);

        assertNotNull(response);
        assertEquals(userId, response.id());
        assertEquals("bielsolosos", response.username());
        assertEquals("biel@dev.com", response.email());
        assertTrue(response.active());
        assertTrue(response.roles().contains("ROLE_USER"));
    }
}
