package br.dev.bielsolosos.biscraper.api.controller.auth;

import br.dev.bielsolosos.biscraper.domain.users.model.dto.SendOtpRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.SendOtpResponse;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.TokenResponse;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.VerifyOtpRequest;
import br.dev.bielsolosos.biscraper.domain.users.service.EmailOtpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmailOtpControllerTest {

    @Mock
    private EmailOtpService emailOtpService;

    @InjectMocks
    private EmailOtpController emailOtpController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(emailOtpController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/v1/auth/otp/send - Deve disparar código OTP e retornar 200 OK")
    void sendOtpSuccess() throws Exception {
        SendOtpRequest request = new SendOtpRequest("bielsolosos");
        SendOtpResponse response = new SendOtpResponse("Código enviado para o e-mail cadastrado.", 600);

        when(emailOtpService.sendOtp(any(SendOtpRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Código enviado para o e-mail cadastrado."))
                .andExpect(jsonPath("$.expiresInSeconds").value(600));
    }

    @Test
    @DisplayName("POST /api/v1/auth/otp/verify - Deve validar código OTP e autenticar")
    void verifyOtpSuccess() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest("bielsolosos", "123456");
        TokenResponse response = new TokenResponse("otp-access-token", "otp-refresh-token");

        when(emailOtpService.verifyOtp(any(VerifyOtpRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("otp-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("otp-refresh-token"));
    }
}
