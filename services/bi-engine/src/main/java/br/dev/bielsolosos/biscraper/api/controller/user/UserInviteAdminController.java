package br.dev.bielsolosos.biscraper.api.controller.user;

import br.dev.bielsolosos.biscraper.domain.users.model.dto.CreateInviteRequest;
import br.dev.bielsolosos.biscraper.domain.users.model.dto.UserInviteResponse;
import br.dev.bielsolosos.biscraper.domain.users.service.UserInviteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Admin User Invites", description = "Endpoints administrativos para geração e gerenciamento de convites de cadastro")
@RestController
@RequestMapping("/api/v1/admin/invites")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserInviteAdminController {

    private final UserInviteService userInviteService;

    @PostMapping
    @Operation(summary = "Gera um novo convite de acesso e dispara e-mail com link exclusivo")
    public ResponseEntity<UserInviteResponse> createInvite(@Valid @RequestBody CreateInviteRequest request) {
        UserInviteResponse response = userInviteService.createInvite(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Lista todos os convites gerados de forma paginada")
    public ResponseEntity<Page<UserInviteResponse>> listInvites(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(userInviteService.listInvites(pageable));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancela um convite pendente")
    public ResponseEntity<Void> cancelInvite(@PathVariable UUID id) {
        userInviteService.cancelInvite(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/resend")
    @Operation(summary = "Renova a validade e reenvia o e-mail de convite")
    public ResponseEntity<UserInviteResponse> resendInvite(@PathVariable UUID id) {
        return ResponseEntity.ok(userInviteService.resendInvite(id));
    }
}
