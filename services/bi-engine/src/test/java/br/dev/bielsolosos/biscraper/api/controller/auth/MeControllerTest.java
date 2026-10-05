package br.dev.bielsolosos.biscraper.api.controller.auth;

import br.dev.bielsolosos.biscraper.core.enums.ModelVendorEnum;
import br.dev.bielsolosos.biscraper.core.enums.RoleEnum;
import br.dev.bielsolosos.biscraper.domain.users.model.Role;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.model.UserConfig;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.AvailableAiModelsResponse;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.UserConfigRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.UserConfigResponse;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.ChangePasswordRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.EditUserRequest;
import br.dev.bielsolosos.biscraper.domain.users.service.UserService;
import br.dev.bielsolosos.biscraper.domain.users.service.UserConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MeControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserConfigService userConfigService;

    @InjectMocks
    private MeController meController;

    private MockMvc mockMvc;
    private User user;
    private UserConfig userConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(meController).build();

        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleEnum.ROLE_USER);

        user = User.builder()
                .id(UUID.randomUUID())
                .username("biel")
                .email("biel@email.com")
                .active(true)
                .roles(Set.of(userRole))
                .build();

        userConfig = UserConfig.builder()
                .id(UUID.randomUUID())
                .user(user)
                .aiVendor(ModelVendorEnum.GEMINI)
                .cheapModel("gemini-2.5-flash")
                .strongModel("gemini-2.5-pro")
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/me - Deve retornar perfil do usuário autenticado com configurações de IA")
    void getProfileSuccess() throws Exception {
        Principal principal = new UsernamePasswordAuthenticationToken("biel", "password", Collections.emptyList());

        when(userService.findUserByUsername("biel")).thenReturn(user);
        when(userConfigService.getConfigForUser(user)).thenReturn(userConfig);

        mockMvc.perform(get("/api/v1/me").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("biel"))
                .andExpect(jsonPath("$.email").value("biel@email.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.config.aiVendor").value("GEMINI"))
                .andExpect(jsonPath("$.config.cheapModel").value("gemini-2.5-flash"))
                .andExpect(jsonPath("$.config.strongModel").value("gemini-2.5-pro"));
    }

    @Test
    @DisplayName("PUT /api/v1/me/configs - Deve atualizar configurações de IA com sucesso")
    void updateConfigSuccess() throws Exception {
        Principal principal = new UsernamePasswordAuthenticationToken("biel", "password", Collections.emptyList());
        UserConfigRequest request = new UserConfigRequest(ModelVendorEnum.DEEPSEEK, "deepseek-chat", "deepseek-reasoner");
        UserConfigResponse response = new UserConfigResponse(userConfig.getId(), ModelVendorEnum.DEEPSEEK, "deepseek-chat", "deepseek-reasoner");

        when(userService.findUserByUsername("biel")).thenReturn(user);
        when(userConfigService.updateConfigForUser(eq(user), any(UserConfigRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/me/configs")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiVendor").value("DEEPSEEK"))
                .andExpect(jsonPath("$.cheapModel").value("deepseek-chat"))
                .andExpect(jsonPath("$.strongModel").value("deepseek-reasoner"));
    }

    @Test
    @DisplayName("GET /api/v1/me/configs/models - Deve retornar catálogo de modelos disponíveis")
    void getAvailableModelsSuccess() throws Exception {
        AvailableAiModelsResponse response = new AvailableAiModelsResponse(List.of(
                new AvailableAiModelsResponse.VendorModelsDto(
                        ModelVendorEnum.GEMINI,
                        "Google Gemini",
                        List.of(new AvailableAiModelsResponse.ModelOptionDto("gemini-2.5-flash", "Gemini 2.5 Flash", "Flash")),
                        List.of(new AvailableAiModelsResponse.ModelOptionDto("gemini-2.5-pro", "Gemini 2.5 Pro", "Pro"))
                )
        ));

        when(userConfigService.getAvailableModelsCatalog()).thenReturn(response);

        mockMvc.perform(get("/api/v1/me/configs/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vendors[0].vendor").value("GEMINI"))
                .andExpect(jsonPath("$.vendors[0].cheapModels[0].id").value("gemini-2.5-flash"));
    }

    @Test
    @DisplayName("PUT /api/v1/me/change-password/{id} - Deve trocar senha com sucesso quando senhas coincidem")
    void changePasswordSuccess() throws Exception {
        Principal principal = new UsernamePasswordAuthenticationToken("biel", "password", Collections.emptyList());
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass123", "oldPass123", "newPass456");

        when(userService.findUserByUsername("biel")).thenReturn(user);

        mockMvc.perform(put("/api/v1/me/change-password/{id}", user.getId())
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Message").value("Senha trocada com sucesso."));

        verify(userService).changePassword(user, "oldPass123", "newPass456");
    }

    @Test
    @DisplayName("PUT /api/v1/me/change-password/{id} - Deve retornar 400 quando confirmação não coincide")
    void changePasswordMismatch() throws Exception {
        Principal principal = new UsernamePasswordAuthenticationToken("biel", "password", Collections.emptyList());
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass123", "differentPass", "newPass456");

        mockMvc.perform(put("/api/v1/me/change-password/{id}", user.getId())
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.Message").value("As senhas não coincidem."));
    }

    @Test
    @DisplayName("POST /api/v1/me/edit-credentials - Deve atualizar dados cadastrais com sucesso")
    void editCredentialsSuccess() throws Exception {
        Principal principal = new UsernamePasswordAuthenticationToken("biel", "password", Collections.emptyList());
        EditUserRequest request = new EditUserRequest("biel_updated", "biel_new@email.com");

        User updatedUser = User.builder()
                .id(user.getId())
                .username("biel_updated")
                .email("biel_new@email.com")
                .active(true)
                .roles(user.getRoles())
                .build();

        when(userService.findUserByUsername("biel")).thenReturn(user);
        when(userService.editUser(user, "biel_updated", "biel_new@email.com")).thenReturn(updatedUser);

        mockMvc.perform(post("/api/v1/me/edit-credentials")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("biel_updated"))
                .andExpect(jsonPath("$.email").value("biel_new@email.com"));
    }
}
