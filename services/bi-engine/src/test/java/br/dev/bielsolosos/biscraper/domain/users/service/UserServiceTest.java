package br.dev.bielsolosos.biscraper.domain.users.service;

import br.dev.bielsolosos.biscraper.core.enums.RoleEnum;
import br.dev.bielsolosos.biscraper.core.exception.BusinessException;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para UserService inspirados na suíte de testes de referência do Noto.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MeService meService;

    @InjectMocks
    private UserService userService;

    private User user;
    private User secondUser;

    @BeforeEach
    void setUp() {
        Role roleUser = Role.builder()
                .id(1L)
                .name(RoleEnum.ROLE_USER)
                .build();

        Role roleAdmin = Role.builder()
                .id(2L)
                .name(RoleEnum.ROLE_ADMIN)
                .build();

        Set<Role> roles = new HashSet<>(Set.of(roleUser, roleAdmin));

        user = User.builder()
                .id(UUID.randomUUID())
                .username("primary-user")
                .email("primary-email@gmail.com")
                .password("senhaAtual")
                .active(true)
                .roles(roles)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        secondUser = User.builder()
                .id(UUID.randomUUID())
                .username("secondary-user")
                .email("secondary-email@gmail.com")
                .password("outraSenha")
                .active(true)
                .roles(new HashSet<>(Set.of(roleUser)))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("changePassword - Deve alterar senha com sucesso quando a senha atual for válida e houver permissão")
    void changePasswordSuccess() {
        when(meService.getMe()).thenReturn(user);
        when(passwordEncoder.matches("senhaAtual", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("novaSenha123")).thenReturn("novaSenhaCriptografada");
        when(repository.save(user)).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.changePassword(user, "senhaAtual", "novaSenha123");

        assertNotNull(result);
        assertEquals("novaSenhaCriptografada", result.getPassword());
        verify(passwordEncoder, times(1)).matches("senhaAtual", "senhaAtual");
        verify(passwordEncoder, times(1)).encode("novaSenha123");
        verify(repository, times(1)).save(user);
    }

    @Test
    @DisplayName("changePassword - Deve lançar BusinessException quando a senha atual for incorreta")
    void changePasswordPasswordIncorrect() {
        when(meService.getMe()).thenReturn(user);
        when(passwordEncoder.matches("senhaErrada", user.getPassword())).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.changePassword(user, "senhaErrada", "novaSenha123"));

        assertEquals("Senha atual incorreta", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("changePassword - Deve lançar BadCredentialsException quando o usuário não tiver permissão")
    void changePasswordWithoutPermission() {
        when(meService.getMe()).thenReturn(secondUser);

        assertThrows(BadCredentialsException.class,
                () -> userService.changePassword(user, "senhaAtual", "novaSenha123"));

        verify(passwordEncoder, never()).matches(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("findUserByUsername - Deve retornar o usuário quando encontrado pelo username")
    void findUserByUsernameSuccess() {
        when(repository.findByUsername("primary-user")).thenReturn(Optional.of(user));

        User result = userService.findUserByUsername("primary-user");

        assertNotNull(result);
        assertEquals("primary-user", result.getUsername());
        verify(repository, times(1)).findByUsername("primary-user");
    }

    @Test
    @DisplayName("findUserByUsername - Deve lançar IllegalArgumentException quando o usuário não for encontrado")
    void findUserByUsernameNotFound() {
        when(repository.findByUsername("ghost-user")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> userService.findUserByUsername("ghost-user"));
    }

    @Test
    @DisplayName("editUser - Deve atualizar username e email com sucesso quando dados forem novos e únicos")
    void editUserSuccess() {
        when(meService.getMe()).thenReturn(user);
        when(repository.findByUsername("novo-username")).thenReturn(Optional.empty());
        when(repository.findByEmail("novo-email@gmail.com")).thenReturn(Optional.empty());
        when(repository.save(user)).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.editUser(user, "novo-username", "novo-email@gmail.com");

        assertNotNull(updated);
        assertEquals("novo-username", updated.getUsername());
        assertEquals("novo-email@gmail.com", updated.getEmail());
        verify(repository, times(1)).save(user);
    }

    @Test
    @DisplayName("editUser - Deve salvar sem checar duplicidade quando username e email forem mantidos iguais")
    void editUserSameUsernameAndEmailSuccess() {
        when(meService.getMe()).thenReturn(user);
        when(repository.save(user)).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userService.editUser(user, user.getUsername(), user.getEmail());

        assertNotNull(updated);
        assertEquals("primary-user", updated.getUsername());
        assertEquals("primary-email@gmail.com", updated.getEmail());
        verify(repository, never()).findByUsername(any());
        verify(repository, never()).findByEmail(any());
        verify(repository, times(1)).save(user);
    }

    @Test
    @DisplayName("editUser - Deve lançar BusinessException quando o novo username já estiver em uso")
    void editUserThrowsBusinessExceptionUsername() {
        when(meService.getMe()).thenReturn(user);
        when(repository.findByUsername(secondUser.getUsername())).thenReturn(Optional.of(secondUser));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.editUser(user, secondUser.getUsername(), "novo-email@gmail.com"));

        assertEquals("Nome de usuário já existente", exception.getMessage());
        verify(repository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("editUser - Deve lançar BusinessException quando o novo email já estiver em uso")
    void editUserThrowsBusinessExceptionEmail() {
        when(meService.getMe()).thenReturn(user);
        when(repository.findByEmail(secondUser.getEmail())).thenReturn(Optional.of(secondUser));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.editUser(user, user.getUsername(), secondUser.getEmail()));

        assertEquals("Email já existente", exception.getMessage());
        verify(repository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("editUser - Deve lançar BadCredentialsException quando o usuário autenticado não for o dono do recurso")
    void editUserWithoutPermission() {
        when(meService.getMe()).thenReturn(secondUser);

        assertThrows(BadCredentialsException.class,
                () -> userService.editUser(user, "novo-username", "novo-email@gmail.com"));

        verify(repository, never()).save(any(User.class));
    }
}
