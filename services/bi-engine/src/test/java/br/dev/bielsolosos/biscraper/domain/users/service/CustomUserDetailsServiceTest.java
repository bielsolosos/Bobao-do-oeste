package br.dev.bielsolosos.biscraper.domain.users.service;

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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User activeUser;
    private User inactiveUser;

    @BeforeEach
    void setUp() {
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleEnum.ROLE_USER);

        Role adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName(RoleEnum.ROLE_ADMIN);

        activeUser = User.builder()
                .id(UUID.randomUUID())
                .username("biel")
                .email("biel@email.com")
                .password("senha")
                .active(true)
                .roles(Set.of(userRole, adminRole))
                .build();

        inactiveUser = User.builder()
                .id(UUID.randomUUID())
                .username("inativo")
                .email("inativo@email.com")
                .password("senha")
                .active(false)
                .roles(Set.of(userRole))
                .build();
    }

    @Test
    @DisplayName("Deve carregar UserDetails com sucesso para usuário ativo")
    void loadUserByUsernameSuccess() {
        when(userRepository.findByUsername(activeUser.getUsername())).thenReturn(Optional.of(activeUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(activeUser.getUsername());

        assertNotNull(userDetails);
        assertEquals(activeUser.getUsername(), userDetails.getUsername());
        assertEquals(activeUser.getPassword(), userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertEquals(2, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_USER".equals(authority.getAuthority())));
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
    }

    @Test
    @DisplayName("Deve carregar UserDetails com isEnabled=false para usuário inativo")
    void loadUserByUsernameInactiveUser() {
        when(userRepository.findByUsername(inactiveUser.getUsername())).thenReturn(Optional.of(inactiveUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(inactiveUser.getUsername());

        assertNotNull(userDetails);
        assertFalse(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException quando usuário não for encontrado")
    void loadUserByUsernameNotFound() {
        when(userRepository.findByUsername("nao-existe")).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("nao-existe")
        );

        assertTrue(ex.getMessage().contains("não encontrado"));
    }
}
