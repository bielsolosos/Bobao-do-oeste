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

    @Operation(summary = "Listar histórico e logs de chamadas de IA do usuário")
    @GetMapping
    public ResponseEntity<Page<AiAnalysisLogResponse>> listAllAiLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.listAllAiLogs(pageable));
    }

}
