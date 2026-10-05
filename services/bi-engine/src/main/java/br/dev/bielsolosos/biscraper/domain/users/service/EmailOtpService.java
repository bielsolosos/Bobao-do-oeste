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
import br.dev.bielsolosos.biscraper.domain.users.notification.EmailOtpNotificationTemplate;
import br.dev.bielsolosos.biscraper.domain.users.repository.EmailLoginOtpRepository;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserRepository;
import br.dev.bielsolosos.biscraper.infrastructure.BiScraperProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("${biscraper.auth.otp.enabled:false} and ${biscraper.email.enabled:false}")
public class EmailOtpService {

    private final UserRepository userRepository;
    private final EmailLoginOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final BiScraperProperties properties;
    private final SecurityUtils jwtUtil;
    private final RefreshTokenService refreshTokenService;

    private final SecureRandom secureRandom = new SecureRandom();

    public boolean isEmailOtpAvailable() {
        return properties.getAuth().getOtp().isEnabled() && properties.getEmail().isEnabled();
    }

    @Transactional
    public SendOtpResponse sendOtp(SendOtpRequest request) {
        if (!isEmailOtpAvailable()) {
            throw new BusinessException("O login por código via e-mail não está habilitado.");
        }

        String identifier = request.identifier().trim();
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BusinessException("O usuário não possui e-mail cadastrado.");
        }

        if (!user.isActive()) {
            throw new BusinessException("Conta de usuário inativa.");
        }

        Optional<EmailLoginOtp> lastOtp = otpRepository.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId());
        if (lastOtp.isPresent()) {
            long elapsedSeconds = Duration.between(lastOtp.get().getCreatedAt(), Instant.now()).getSeconds();
            int cooldown = properties.getAuth().getOtp().getCooldownSeconds();
            if (elapsedSeconds < cooldown) {
                long remaining = cooldown - elapsedSeconds;
                throw new BusinessException("Aguarde " + remaining + " segundos antes de solicitar um novo código.");
            }
        }

        int randomCode = 100000 + secureRandom.nextInt(900000);
        String code = String.valueOf(randomCode);

        int expirationMinutes = properties.getAuth().getOtp().getExpirationMinutes();
        Instant expiresAt = Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES);

        EmailLoginOtp otp = EmailLoginOtp.builder()
                .user(user)
                .codeHash(passwordEncoder.encode(code))
                .expiresAt(expiresAt)
                .attempts(0)
                .used(false)
                .build();

        otpRepository.save(otp);

        // Despacha notificação desacoplada via pipeline de eventos existente
        EmailOtpNotificationTemplate template = new EmailOtpNotificationTemplate(user, code, expirationMinutes);
        NotificationEvent event = NotificationEvent.builder()
                .recipient(user)
                .contentTemplate(template)
                .transactional(true)
                .build();

        eventPublisher.publishEvent(event);

        log.info("Código OTP de login gerado e evento de e-mail publicado para o usuário: '{}'", user.getUsername());
        return new SendOtpResponse("Código de acesso enviado para o e-mail cadastrado.", expirationMinutes * 60);
    }

    @Transactional
    public TokenResponse verifyOtp(VerifyOtpRequest request) {
        if (!isEmailOtpAvailable()) {
            throw new BusinessException("O login por código via e-mail não está habilitado.");
        }

        String identifier = request.identifier().trim();
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));

        EmailLoginOtp otp = otpRepository.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new BusinessException("Nenhum código ativo encontrado para este usuário. Solicite um novo código."));

        if (otp.getExpiresAt().isBefore(Instant.now())) {
            otp.setUsed(true);
            otpRepository.save(otp);
            throw new BusinessException("O código de acesso expirou. Solicite um novo.");
        }

        int maxAttempts = properties.getAuth().getOtp().getMaxAttempts();
        if (otp.getAttempts() >= maxAttempts) {
            otp.setUsed(true);
            otpRepository.save(otp);
            throw new BusinessException("Número máximo de tentativas excedido. Solicite um novo código.");
        }

        boolean matches = passwordEncoder.matches(request.code().trim(), otp.getCodeHash());
        if (!matches) {
            otp.setAttempts(otp.getAttempts() + 1);
            int remainingAttempts = maxAttempts - otp.getAttempts();

            if (remainingAttempts <= 0) {
                otp.setUsed(true);
                otpRepository.save(otp);
                throw new BusinessException("Código incorreto. Limite de tentativas excedido. Solicite um novo código.");
            }

            otpRepository.save(otp);
            throw new BusinessException("Código de verificação incorreto. Restam " + remainingAttempts + " tentativa(s).");
        }

        otp.setUsed(true);
        otpRepository.save(otp);

        String token = jwtUtil.generateToken(user.getUsername());
        String refreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        log.info("Autenticação OTP bem-sucedida para o usuário: '{}'", user.getUsername());
        return new TokenResponse(token, refreshToken);
    }
}
