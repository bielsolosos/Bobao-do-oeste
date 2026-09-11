package br.dev.bielsolosos.biscraper.api.controller.ai;

import br.dev.bielsolosos.biscraper.domain.ai.model.dto.AiAnalysisLogResponse;
import br.dev.bielsolosos.biscraper.domain.ai.service.AiAnalysisLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "AI Logs", description = "Endpoints para gerenciamento de logs e histórico da IA.")
@RestController
@RequestMapping("/api/v1/ai-logs")
@RequiredArgsConstructor
public class AiAnalysisLogController {

    private final AiAnalysisLogService service;

    @Operation(summary = "Listar histórico e logs de chamadas de IA do usuário, com filtros opcionais por status e busca")
    @GetMapping
    public ResponseEntity<Page<AiAnalysisLogResponse>> listAllAiLogs(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "q", required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.listAllAiLogs(status, keyword, pageable));
    }

    @Operation(summary = "Listar logs de IA de um anúncio específico (para visualizar o 'pensamento' da IA)")
    @GetMapping("/by-listing/{listingId}")
    public ResponseEntity<Page<AiAnalysisLogResponse>> listAiLogsByListing(
            @PathVariable UUID listingId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(service.listLogsByListing(listingId, pageable));
    }

}
