package br.dev.bielsolosos.biscraper.api.controller.auth;

import br.dev.bielsolosos.biscraper.domain.users.model.dto.SendOtpRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.SendOtpResponse;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.TokenResponse;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.VerifyOtpRequest;
import br.dev.bielsolosos.biscraper.domain.users.service.EmailOtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/otp")
@RequiredArgsConstructor
@Tag(name = "Auth OTP", description = "Endpoints de autenticação Passwordless via código OTP por e-mail")
@ConditionalOnExpression("${biscraper.auth.otp.enabled:false} and ${biscraper.email.enabled:false}")
public class EmailOtpController {

    private final EmailOtpService emailOtpService;

    @PostMapping("/send")
    @Operation(summary = "Solicita o envio de um código OTP por e-mail para login")
    public ResponseEntity<SendOtpResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        log.info("Solicitação de código OTP recebida para identificador: '{}'", request.identifier());
        SendOtpResponse response = emailOtpService.sendOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    @Operation(summary = "Valida o código OTP e efetua login retornando os tokens JWT")
    public ResponseEntity<TokenResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("Tentativa de validação de código OTP para identificador: '{}'", request.identifier());
        TokenResponse response = emailOtpService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }
}
