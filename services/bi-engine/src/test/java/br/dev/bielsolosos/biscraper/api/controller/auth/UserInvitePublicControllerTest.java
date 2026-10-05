package br.dev.bielsolosos.biscraper.api.controller.auth;

import br.dev.bielsolosos.biscraper.domain.users.model.dto.AcceptInviteRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.TokenResponse;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.ValidateInviteResponse;
import br.dev.bielsolosos.biscraper.domain.users.service.UserInviteService;
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

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserInvitePublicControllerTest {

    @Mock
    private UserInviteService userInviteService;

    @InjectMocks
    private UserInvitePublicController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("GET /api/v1/auth/invites/validate - Deve validar token e retornar 200 OK")
    void validateInviteSuccess() throws Exception {
        ValidateInviteResponse response = new ValidateInviteResponse("teste@empresa.com", true, Instant.now().plusSeconds(3600));

        when(userInviteService.validateInvite("token123")).thenReturn(response);

        mockMvc.perform(get("/api/v1/auth/invites/validate?token=token123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("teste@empresa.com"))
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/auth/invites/accept - Deve aceitar convite e retornar 200 OK com tokens")
    void acceptInviteSuccess() throws Exception {
        AcceptInviteRequest request = new AcceptInviteRequest("token123", "novouser", "senhaForte123");
        TokenResponse response = new TokenResponse("access-token-ok", "refresh-token-ok");

        when(userInviteService.acceptInvite(any(AcceptInviteRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/invites/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access-token-ok"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-ok"));
    }
}
