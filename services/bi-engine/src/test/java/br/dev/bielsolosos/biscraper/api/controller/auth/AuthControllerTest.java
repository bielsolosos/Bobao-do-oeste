package br.dev.bielsolosos.biscraper.api.controller.auth;

import br.dev.bielsolosos.biscraper.domain.users.model.dto.LoginRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.RefreshRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.TokenResponse;
import br.dev.bielsolosos.biscraper.domain.users.service.AuthService;
import br.dev.bielsolosos.biscraper.infrastructure.BiScraperProperties;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private BiScraperProperties properties;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GET /api/v1/auth/config - Deve retornar disponibilidade das features de autenticação")
    void getAuthConfigSuccess() throws Exception {
        BiScraperProperties.Auth auth = new BiScraperProperties.Auth();
        auth.getOtp().setEnabled(true);
        BiScraperProperties.Email email = new BiScraperProperties.Email();
        email.setEnabled(true);

        when(properties.getAuth()).thenReturn(auth);
        when(properties.getEmail()).thenReturn(email);

        mockMvc.perform(get("/api/v1/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailOtpEnabled").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Deve autenticar e retornar 200 OK com tokens")
    void loginSuccess() throws Exception {
        LoginRequest request = new LoginRequest("biel", "senha123");
        TokenResponse response = new TokenResponse("access-token", "refresh-token");

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Deve renovar e retornar 200 OK com novos tokens")
    void refreshSuccess() throws Exception {
        RefreshRequest request = new RefreshRequest("valid-refresh-token");
        TokenResponse response = new TokenResponse("new-access-token", "new-refresh-token");

        when(authService.refresh(any(RefreshRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }
}
