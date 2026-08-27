package br.dev.bielsolosos.biscraper.api.controller.auth;

import br.dev.bielsolosos.biscraper.core.enums.RoleEnum;
import br.dev.bielsolosos.biscraper.domain.users.model.Role;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MeControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MeController meController;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(meController).build();

        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleEnum.ROLE_USER);

        user = User.builder()
                .id(UUID.randomUUID())
                .username("biel")
                .email("biel@email.com")
                .active(true)
                .roles(Set.of(userRole))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/me - Deve retornar perfil do usuário autenticado")
    void getProfileSuccess() throws Exception {
        Principal principal = new UsernamePasswordAuthenticationToken("biel", "password", Collections.emptyList());

        when(userRepository.findByUsername("biel")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/me").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("biel"))
                .andExpect(jsonPath("$.email").value("biel@email.com"))
                .andExpect(jsonPath("$.active").value(true));
    }
}
