package br.dev.bielsolosos.biscraper.domain.users.service;

import br.dev.bielsolosos.biscraper.core.exception.BusinessException;
import br.dev.bielsolosos.biscraper.core.utils.SecurityUtils;
import br.dev.bielsolosos.biscraper.domain.notification.event.NotificationEvent;
import br.dev.bielsolosos.biscraper.domain.users.model.EmailLoginOtp;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.SendOtpRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.SendOtpResponse;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.TokenResponse;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.VerifyOtpRequest;
import br.dev.bielsolosos.biscraper.domain.users.repository.EmailLoginOtpRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailOtpServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailLoginOtpRepository otpRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private BiScraperProperties properties;

    @Mock
    private SecurityUtils jwtUtil;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private EmailOtpService emailOtpService;

    private User user;
    private BiScraperProperties.Auth authProps;
    private BiScraperProperties.Email emailProps;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("bielsolosos")
                .email("biel@email.com")
                .active(true)
                .build();

        authProps = new BiScraperProperties.Auth();
        authProps.getOtp().setEnabled(true);
        authProps.getOtp().setExpirationMinutes(10);
        authProps.getOtp().setMaxAttempts(5);
        authProps.getOtp().setCooldownSeconds(60);

        emailProps = new BiScraperProperties.Email();
        emailProps.setEnabled(true);
        emailProps.setFrom("BI Scraper <teste@bi.com>");
    }

    private void mockFeatureEnabled() {
        when(properties.getAuth()).thenReturn(authProps);
        when(properties.getEmail()).thenReturn(emailProps);
    }

    @Test
    @DisplayName("sendOtp - Deve gerar OTP e publicar NotificationEvent de e-mail com sucesso")
    void sendOtpSuccess() {
        mockFeatureEnabled();

        when(userRepository.findByUsername("bielsolosos")).thenReturn(Optional.of(user));
        when(otpRepository.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashedCode");

        SendOtpResponse response = emailOtpService.sendOtp(new SendOtpRequest("bielsolosos"));

        assertNotNull(response);
        assertEquals(600, response.expiresInSeconds());
        verify(otpRepository).save(any(EmailLoginOtp.class));
        verify(eventPublisher).publishEvent(any(NotificationEvent.class));
    }

    @Test
    @DisplayName("sendOtp - Deve lançar BusinessException quando a feature estiver desabilitada")
    void sendOtpFeatureDisabled() {
        when(properties.getAuth()).thenReturn(authProps);
        authProps.getOtp().setEnabled(false);

        assertThrows(BusinessException.class, () -> emailOtpService.sendOtp(new SendOtpRequest("bielsolosos")));
        verify(otpRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("sendOtp - Deve lançar BusinessException quando o usuário não existir")
    void sendOtpUserNotFound() {
        mockFeatureEnabled();
        when(userRepository.findByUsername("desconhecido")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("desconhecido")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> emailOtpService.sendOtp(new SendOtpRequest("desconhecido")));
    }

    @Test
    @DisplayName("sendOtp - Deve lançar BusinessException quando o cooldown de reenvio estiver ativo")
    void sendOtpCooldownActive() {
        mockFeatureEnabled();

        EmailLoginOtp recentOtp = EmailLoginOtp.builder()
                .user(user)
                .codeHash("hash")
                .createdAt(Instant.now().minus(20, ChronoUnit.SECONDS))
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .used(false)
                .build();

        when(userRepository.findByUsername("bielsolosos")).thenReturn(Optional.of(user));
        when(otpRepository.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.of(recentOtp));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> emailOtpService.sendOtp(new SendOtpRequest("bielsolosos")));

        assertTrue(exception.getMessage().contains("segundos"));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("verifyOtp - Deve autenticar com sucesso e retornar tokens JWT quando código conferir")
    void verifyOtpSuccess() {
        mockFeatureEnabled();

        EmailLoginOtp otp = EmailLoginOtp.builder()
                .id(UUID.randomUUID())
                .user(user)
                .codeHash("hashValid")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .attempts(0)
                .used(false)
                .build();

        when(userRepository.findByUsername("bielsolosos")).thenReturn(Optional.of(user));
        when(otpRepository.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("123456", "hashValid")).thenReturn(true);
        when(jwtUtil.generateToken("bielsolosos")).thenReturn("jwtToken");
        when(refreshTokenService.createRefreshToken("bielsolosos")).thenReturn("refreshToken");

        TokenResponse response = emailOtpService.verifyOtp(new VerifyOtpRequest("bielsolosos", "123456"));

        assertNotNull(response);
        assertEquals("jwtToken", response.token());
        assertEquals("refreshToken", response.refreshToken());
        assertTrue(otp.isUsed());
        verify(otpRepository).save(otp);
    }

    @Test
    @DisplayName("verifyOtp - Deve lançar BusinessException quando o código estiver expirado")
    void verifyOtpExpired() {
        mockFeatureEnabled();

        EmailLoginOtp otp = EmailLoginOtp.builder()
                .id(UUID.randomUUID())
                .user(user)
                .codeHash("hashValid")
                .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .attempts(0)
                .used(false)
                .build();

        when(userRepository.findByUsername("bielsolosos")).thenReturn(Optional.of(user));
        when(otpRepository.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.of(otp));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> emailOtpService.verifyOtp(new VerifyOtpRequest("bielsolosos", "123456")));

        assertTrue(exception.getMessage().contains("expirou"));
        assertTrue(otp.isUsed());
        verify(otpRepository).save(otp);
    }

    @Test
    @DisplayName("verifyOtp - Deve lançar BusinessException e incrementar tentativas quando código for incorreto")
    void verifyOtpIncorrectCode() {
        mockFeatureEnabled();

        EmailLoginOtp otp = EmailLoginOtp.builder()
                .id(UUID.randomUUID())
                .user(user)
                .codeHash("hashValid")
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .attempts(1)
                .used(false)
                .build();

        when(userRepository.findByUsername("bielsolosos")).thenReturn(Optional.of(user));
        when(otpRepository.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())).thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("999999", "hashValid")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> emailOtpService.verifyOtp(new VerifyOtpRequest("bielsolosos", "999999")));

        assertTrue(exception.getMessage().toLowerCase().contains("restam"));
        assertEquals(2, otp.getAttempts());
        assertFalse(otp.isUsed());
        verify(otpRepository).save(otp);
    }
}
