package br.dev.bielsolosos.biscraper.domain.users.service;

import br.dev.bielsolosos.biscraper.core.exception.BusinessException;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MeService meService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("bielsolosos")
                .email("biel@dev.com")
                .active(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve retornar o usuário autenticado com sucesso")
    void shouldReturnAuthenticatedUser() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("bielsolosos", "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByUsername("bielsolosos")).thenReturn(Optional.of(user));

        User result = meService.getMe();

        assertNotNull(result);
        assertEquals("bielsolosos", result.getUsername());
        verify(userRepository, times(1)).findByUsername("bielsolosos");
    }

    @Test
    @DisplayName("Deve lançar exceção quando não houver autenticação no contexto")
    void shouldThrowExceptionWhenNoAuthentication() {
        SecurityContextHolder.clearContext();

        BusinessException ex = assertThrows(BusinessException.class, () -> meService.getMe());
        assertEquals("Usuário não autenticado", ex.getMessage());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o usuário autenticado não existir no banco")
    void shouldThrowExceptionWhenUserNotFoundInDatabase() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("ghost_user", "password", Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByUsername("ghost_user")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> meService.getMe());
        assertEquals("Usuário não encontrado", ex.getMessage());
    }
}
