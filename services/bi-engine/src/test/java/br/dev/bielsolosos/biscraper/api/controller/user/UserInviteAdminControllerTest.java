package br.dev.bielsolosos.biscraper.api.controller.user;

import br.dev.bielsolosos.biscraper.core.enums.UserInviteStatus;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.CreateInviteRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.UserInviteResponse;
import br.dev.bielsolosos.biscraper.domain.users.service.UserInviteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserInviteAdminControllerTest {

    @Mock
    private UserInviteService userInviteService;

    @InjectMocks
    private UserInviteAdminController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/v1/admin/invites - Deve criar convite e retornar 201 CREATED")
    void createInviteSuccess() throws Exception {
        CreateInviteRequest request = new CreateInviteRequest("teste@empresa.com", "ROLE_USER");
        UserInviteResponse response = new UserInviteResponse(
                UUID.randomUUID(),
                "teste@empresa.com",
                "ROLE_USER",
                UserInviteStatus.PENDING,
                Instant.now().plusSeconds(3600),
                null,
                Instant.now(),
                "admin",
                null
        );

        when(userInviteService.createInvite(any(CreateInviteRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/invites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("teste@empresa.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/invites - Deve listar convites e retornar 200 OK")
    void listInvitesSuccess() throws Exception {
        UserInviteResponse response = new UserInviteResponse(
                UUID.randomUUID(),
                "teste@empresa.com",
                "ROLE_USER",
                UserInviteStatus.PENDING,
                Instant.now().plusSeconds(3600),
                null,
                Instant.now(),
                "admin",
                null
        );

        when(userInviteService.listInvites(any(Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(response), org.springframework.data.domain.PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/admin/invites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("teste@empresa.com"));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/invites/{id} - Deve cancelar convite e retornar 204 NO_CONTENT")
    void cancelInviteSuccess() throws Exception {
        UUID inviteId = UUID.randomUUID();
        doNothing().when(userInviteService).cancelInvite(inviteId);

        mockMvc.perform(delete("/api/v1/admin/invites/" + inviteId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/admin/invites/{id}/resend - Deve reenviar convite e retornar 200 OK")
    void resendInviteSuccess() throws Exception {
        UUID inviteId = UUID.randomUUID();
        UserInviteResponse response = new UserInviteResponse(
                inviteId,
                "teste@empresa.com",
                "ROLE_USER",
                UserInviteStatus.PENDING,
                Instant.now().plusSeconds(3600),
                null,
                Instant.now(),
                "admin",
                null
        );

        when(userInviteService.resendInvite(inviteId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/invites/" + inviteId + "/resend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(inviteId.toString()));
    }
}
