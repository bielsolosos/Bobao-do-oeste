package br.dev.bielsolosos.biscraper.domain.users.service;

import br.dev.bielsolosos.biscraper.core.enums.RoleEnum;
import br.dev.bielsolosos.biscraper.core.enums.UserInviteStatus;
import br.dev.bielsolosos.biscraper.core.exception.BusinessException;
import br.dev.bielsolosos.biscraper.core.utils.SecurityUtils;
import br.dev.bielsolosos.biscraper.domain.notification.event.NotificationEvent;
import br.dev.bielsolosos.biscraper.domain.users.model.Role;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.model.UserInvite;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.*;
import br.dev.bielsolosos.biscraper.domain.users.repository.RoleRepository;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserInviteRepository;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserRepository;
import br.dev.bielsolosos.biscraper.infrastructure.BiScraperProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserInviteServiceTest {

    @Mock
    private UserInviteRepository inviteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserConfigService userConfigService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityUtils jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private BiScraperProperties properties;

    @Mock
    private MeService meService;

    @InjectMocks
    private UserInviteService userInviteService;

    private User admin;
    private BiScraperProperties.Auth authProps;

    @BeforeEach
    void setUp() {
        admin = User.builder()
                .id(UUID.randomUUID())
                .username("admin-master")
                .email("admin@master.com")
                .active(true)
                .build();

        authProps = new BiScraperProperties.Auth();
        authProps.getInvites().setEnabled(true);
        authProps.getInvites().setExpirationHours(48);

        lenient().when(properties.getAuth()).thenReturn(authProps);
        lenient().when(properties.getAppUrl()).thenReturn("https://bi.bielsolosos.dev.br");
    }

    @Test
    @DisplayName("createInvite - Deve gerar convite e disparar e-mail com sucesso")
    void createInviteSuccess() {
        when(meService.getMe()).thenReturn(admin);
        when(userRepository.findByEmail("novo@empresa.com")).thenReturn(Optional.empty());
        when(inviteRepository.findAllByEmailAndStatus("novo@empresa.com", UserInviteStatus.PENDING)).thenReturn(List.of());
        when(inviteRepository.save(any(UserInvite.class))).thenAnswer(invocation -> {
            UserInvite inv = invocation.getArgument(0);
            inv.setId(UUID.randomUUID());
            return inv;
        });

        UserInviteResponse response = userInviteService.createInvite(new CreateInviteRequest("novo@empresa.com", "ROLE_USER"));

        assertNotNull(response);
        assertEquals("novo@empresa.com", response.email());
        assertEquals("ROLE_USER", response.role());
        assertEquals(UserInviteStatus.PENDING, response.status());
        assertEquals("admin-master", response.invitedByUsername());
        verify(inviteRepository, atLeastOnce()).save(any(UserInvite.class));
        verify(eventPublisher, times(1)).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("createInvite - Deve lançar BusinessException quando o e-mail já estiver cadastrado")
    void createInviteEmailAlreadyExists() {
        when(meService.getMe()).thenReturn(admin);
        when(userRepository.findByEmail("existente@empresa.com")).thenReturn(Optional.of(User.builder().build()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userInviteService.createInvite(new CreateInviteRequest("existente@empresa.com", "ROLE_USER")));

        assertTrue(exception.getMessage().contains("Já existe um usuário cadastrado"));
        verify(inviteRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("createInvite - Deve cancelar convite pendente anterior se existir para o mesmo e-mail")
    void createInviteCancelsPreviousPendingInvite() {
        when(meService.getMe()).thenReturn(admin);
        when(userRepository.findByEmail("duplicado@empresa.com")).thenReturn(Optional.empty());

        UserInvite oldPending = UserInvite.builder()
                .id(UUID.randomUUID())
                .email("duplicado@empresa.com")
                .status(UserInviteStatus.PENDING)
                .build();

        when(inviteRepository.findAllByEmailAndStatus("duplicado@empresa.com", UserInviteStatus.PENDING))
                .thenReturn(List.of(oldPending));
        when(inviteRepository.save(any(UserInvite.class))).thenAnswer(inv -> inv.getArgument(0));

        userInviteService.createInvite(new CreateInviteRequest("duplicado@empresa.com", null));

        assertEquals(UserInviteStatus.CANCELLED, oldPending.getStatus());
        verify(inviteRepository, times(2)).save(any(UserInvite.class));
    }

    @Test
    @DisplayName("validateInvite - Deve validar convite com sucesso")
    void validateInviteSuccess() {
        UserInvite invite = UserInvite.builder()
                .token("token123")
                .email("convidado@empresa.com")
                .status(UserInviteStatus.PENDING)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();

        when(inviteRepository.findByToken("token123")).thenReturn(Optional.of(invite));

        ValidateInviteResponse response = userInviteService.validateInvite("token123");

        assertNotNull(response);
        assertEquals("convidado@empresa.com", response.email());
        assertTrue(response.valid());
    }

    @Test
    @DisplayName("validateInvite - Deve lançar BusinessException quando o convite estiver expirado")
    void validateInviteExpired() {
        UserInvite invite = UserInvite.builder()
                .token("tokenExpired")
                .email("convidado@empresa.com")
                .status(UserInviteStatus.PENDING)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        when(inviteRepository.findByToken("tokenExpired")).thenReturn(Optional.of(invite));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userInviteService.validateInvite("tokenExpired"));

        assertTrue(exception.getMessage().contains("expirou"));
        assertEquals(UserInviteStatus.EXPIRED, invite.getStatus());
        verify(inviteRepository).save(invite);
    }

    @Test
    @DisplayName("acceptInvite - Deve criar novo usuário e retornar tokens JWT com sucesso")
    void acceptInviteSuccess() {
        UserInvite invite = UserInvite.builder()
                .token("tokenValido")
                .email("novo.membro@empresa.com")
                .role("ROLE_USER")
                .status(UserInviteStatus.PENDING)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();

        Role role = Role.builder().id(1L).name(RoleEnum.ROLE_USER).build();

        when(inviteRepository.findByToken("tokenValido")).thenReturn(Optional.of(invite));
        when(userRepository.findByUsername("novomembro")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("novo.membro@empresa.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleEnum.ROLE_USER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("senhaForte123")).thenReturn("encodedPassword");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        when(jwtUtil.generateToken("novomembro")).thenReturn("jwt-token-ok");
        when(refreshTokenService.createRefreshToken("novomembro")).thenReturn("refresh-token-ok");

        TokenResponse tokenResponse = userInviteService.acceptInvite(
                new AcceptInviteRequest("tokenValido", "novomembro", "senhaForte123")
        );

        assertNotNull(tokenResponse);
        assertEquals("jwt-token-ok", tokenResponse.token());
        assertEquals("refresh-token-ok", tokenResponse.refreshToken());
        assertEquals(UserInviteStatus.ACCEPTED, invite.getStatus());
        assertNotNull(invite.getAcceptedAt());
        assertNotNull(invite.getAcceptedBy());

        verify(userRepository).save(any(User.class));
        verify(userConfigService).getConfigForUser(any(User.class));
        verify(inviteRepository).save(invite);
    }

    @Test
    @DisplayName("cancelInvite - Deve cancelar convite pendente com sucesso")
    void cancelInviteSuccess() {
        UUID inviteId = UUID.randomUUID();
        UserInvite invite = UserInvite.builder()
                .id(inviteId)
                .email("cancela@empresa.com")
                .status(UserInviteStatus.PENDING)
                .build();

        when(inviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));

        userInviteService.cancelInvite(inviteId);

        assertEquals(UserInviteStatus.CANCELLED, invite.getStatus());
        verify(inviteRepository).save(invite);
    }

    @Test
    @DisplayName("resendInvite - Deve renovar prazo e reenviar e-mail")
    void resendInviteSuccess() {
        when(meService.getMe()).thenReturn(admin);
        UUID inviteId = UUID.randomUUID();
        UserInvite invite = UserInvite.builder()
                .id(inviteId)
                .email("reenviar@empresa.com")
                .token("oldToken")
                .status(UserInviteStatus.PENDING)
                .build();

        when(inviteRepository.findById(inviteId)).thenReturn(Optional.of(invite));
        when(inviteRepository.save(any(UserInvite.class))).thenAnswer(i -> i.getArgument(0));

        UserInviteResponse response = userInviteService.resendInvite(inviteId);

        assertNotNull(response);
        assertNotEquals("oldToken", invite.getToken());
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("listInvites - Deve retornar página de convites")
    void listInvitesSuccess() {
        UserInvite invite = UserInvite.builder()
                .id(UUID.randomUUID())
                .email("teste@teste.com")
                .role("ROLE_USER")
                .status(UserInviteStatus.PENDING)
                .build();

        when(inviteRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(invite)));

        Page<UserInviteResponse> result = userInviteService.listInvites(PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("teste@teste.com", result.getContent().get(0).email());
    }
}
