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
import br.dev.bielsolosos.biscraper.domain.users.notification.UserInviteNotificationTemplate;
import br.dev.bielsolosos.biscraper.domain.users.repository.RoleRepository;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserInviteRepository;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserRepository;
import br.dev.bielsolosos.biscraper.infrastructure.BiScraperProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserInviteService {

    private final UserInviteRepository inviteRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserConfigService userConfigService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final BiScraperProperties properties;
    private final MeService meService;

    @Transactional
    public UserInviteResponse createInvite(CreateInviteRequest request) {
        if (!properties.getAuth().getInvites().isEnabled()) {
            throw new BusinessException("A criação de convites está desativada no sistema.");
        }

        User admin = meService.getMe();
        String email = request.email().trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("Já existe um usuário cadastrado com este e-mail.");
        }

        // Cancela convites pendentes anteriores para o mesmo e-mail
        List<UserInvite> pendingInvites = inviteRepository.findAllByEmailAndStatus(email, UserInviteStatus.PENDING);
        for (UserInvite oldInvite : pendingInvites) {
            oldInvite.setStatus(UserInviteStatus.CANCELLED);
            inviteRepository.save(oldInvite);
        }

        String roleName = (request.role() != null && !request.role().isBlank())
                ? request.role().trim().toUpperCase()
                : RoleEnum.ROLE_USER.name();

        try {
            RoleEnum.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Perfil de acesso inválido: " + roleName);
        }

        int hours = properties.getAuth().getInvites().getExpirationHours();
        Instant expiresAt = Instant.now().plus(hours, ChronoUnit.HOURS);
        String token = generateSecureToken();

        UserInvite invite = UserInvite.builder()
                .email(email)
                .token(token)
                .role(roleName)
                .status(UserInviteStatus.PENDING)
                .invitedBy(admin)
                .expiresAt(expiresAt)
                .build();

        UserInvite saved = inviteRepository.save(invite);

        sendInviteEmail(saved, admin.getUsername(), hours);

        log.info("Convite criado com sucesso para '{}' por '{}'", email, admin.getUsername());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<UserInviteResponse> listInvites(Pageable pageable) {
        return inviteRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    @Transactional
    public void cancelInvite(UUID inviteId) {
        UserInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new BusinessException("Convite não encontrado."));

        if (invite.getStatus() != UserInviteStatus.PENDING) {
            throw new BusinessException("Apenas convites com status PENDENTE podem ser cancelados.");
        }

        invite.setStatus(UserInviteStatus.CANCELLED);
        inviteRepository.save(invite);
        log.info("Convite {} para '{}' cancelado.", inviteId, invite.getEmail());
    }

    @Transactional
    public UserInviteResponse resendInvite(UUID inviteId) {
        UserInvite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new BusinessException("Convite não encontrado."));

        if (invite.getStatus() == UserInviteStatus.ACCEPTED) {
            throw new BusinessException("Este convite já foi aceito e não pode ser reenviado.");
        }

        User admin = meService.getMe();
        int hours = properties.getAuth().getInvites().getExpirationHours();

        invite.setToken(generateSecureToken());
        invite.setExpiresAt(Instant.now().plus(hours, ChronoUnit.HOURS));
        invite.setStatus(UserInviteStatus.PENDING);
        invite.setInvitedBy(admin);

        UserInvite saved = inviteRepository.save(invite);
        sendInviteEmail(saved, admin.getUsername(), hours);

        log.info("Convite {} reenviado para '{}'", inviteId, invite.getEmail());
        return toResponse(saved);
    }

    @Transactional
    public ValidateInviteResponse validateInvite(String token) {
        UserInvite invite = findAndVerifyInvite(token);
        return new ValidateInviteResponse(invite.getEmail(), true, invite.getExpiresAt());
    }

    @Transactional
    public TokenResponse acceptInvite(AcceptInviteRequest request) {
        UserInvite invite = findAndVerifyInvite(request.token().trim());

        String username = request.username().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException("O nome de usuário '" + username + "' já está em uso.");
        }

        if (userRepository.findByEmail(invite.getEmail()).isPresent()) {
            throw new BusinessException("Já existe uma conta cadastrada com este e-mail.");
        }

        RoleEnum roleEnum = RoleEnum.valueOf(invite.getRole());
        Role role = roleRepository.findByName(roleEnum)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleEnum).build()));

        User newUser = User.builder()
                .username(username)
                .email(invite.getEmail())
                .password(passwordEncoder.encode(request.password()))
                .active(true)
                .roles(new HashSet<>(Set.of(role)))
                .build();

        User savedUser = userRepository.save(newUser);

        // Inicializa configurações padrão de usuário (UserConfig)
        userConfigService.getConfigForUser(savedUser);

        // Atualiza convite para ACCEPTED
        invite.setStatus(UserInviteStatus.ACCEPTED);
        invite.setAcceptedAt(Instant.now());
        invite.setAcceptedBy(savedUser);
        inviteRepository.save(invite);

        // Gera tokens JWT para login automático
        String accessToken = jwtUtil.generateToken(savedUser.getUsername());
        String refreshToken = refreshTokenService.createRefreshToken(savedUser.getUsername());

        log.info("Convite aceito com sucesso! Novo usuário registrado: '{}' ({})", username, savedUser.getEmail());
        return new TokenResponse(accessToken, refreshToken);
    }

    private UserInvite findAndVerifyInvite(String token) {
        UserInvite invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Convite inválido ou não encontrado."));

        if (invite.getStatus() == UserInviteStatus.ACCEPTED) {
            throw new BusinessException("Este convite já foi utilizado.");
        }

        if (invite.getStatus() == UserInviteStatus.CANCELLED) {
            throw new BusinessException("Este convite foi cancelado pelo administrador.");
        }

        if (invite.getExpiresAt().isBefore(Instant.now())) {
            invite.setStatus(UserInviteStatus.EXPIRED);
            inviteRepository.save(invite);
            throw new BusinessException("Este convite expirou. Solicite um novo convite ao administrador.");
        }

        return invite;
    }

    private void sendInviteEmail(UserInvite invite, String adminUsername, int hours) {
        String appUrl = properties.getAppUrl().replaceAll("/+$", "");
        String inviteUrl = appUrl + "/accept-invite?token=" + invite.getToken();

        UserInviteNotificationTemplate template = new UserInviteNotificationTemplate(
                invite.getEmail(),
                inviteUrl,
                hours,
                adminUsername
        );

        User transientRecipient = User.builder()
                .email(invite.getEmail())
                .username(invite.getEmail())
                .build();

        NotificationEvent event = NotificationEvent.builder()
                .recipient(transientRecipient)
                .contentTemplate(template)
                .transactional(true)
                .build();

        eventPublisher.publishEvent(event);
    }

    private String generateSecureToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

    public UserInviteResponse toResponse(UserInvite invite) {
        return new UserInviteResponse(
                invite.getId(),
                invite.getEmail(),
                invite.getRole(),
                invite.getStatus(),
                invite.getExpiresAt(),
                invite.getAcceptedAt(),
                invite.getCreatedAt(),
                invite.getInvitedBy() != null ? invite.getInvitedBy().getUsername() : null,
                invite.getAcceptedBy() != null ? invite.getAcceptedBy().getUsername() : null
        );
    }
}
