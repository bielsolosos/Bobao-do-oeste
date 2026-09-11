package br.dev.bielsolosos.biscraper.api.controller.productmonitor;

import br.dev.bielsolosos.biscraper.domain.ai.service.AiAnalysisLogService;
import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.monitor.ProductMonitorRequest;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.monitor.ProductMonitorResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapedListingResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.service.ProductMonitorService;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Product Monitor", description = "Endpoints para gerenciamento completo dos monitores de produtos.")
@RestController
@RequestMapping("/api/v1/product-monitors")
@RequiredArgsConstructor
public class ProductMonitorController {

    private final ProductMonitorService service;
    private final AiAnalysisLogService aiAnalysisLogService;

    @Operation(summary = "Criar um novo monitor de busca com parâmetros e IA")
    @PostMapping
    public ResponseEntity<ProductMonitorResponse> create(@Valid @RequestBody ProductMonitorRequest request) {
        ProductMonitorResponse response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Atualizar configurações, termos e filtros de um monitor existente")
    @PutMapping("/{id}")
    public ResponseEntity<ProductMonitorResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductMonitorRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @Operation(summary = "Listagem paginada dos monitores do usuário autenticado")
    @GetMapping
    public ResponseEntity<Page<ProductMonitorResponse>> list(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.listPaged(pageable));
    }

    @Operation(summary = "Listar todos os anúncios extraídos de todos os monitores do usuário")
    @GetMapping("/listings")
    public ResponseEntity<Page<ScrapedListingResponse>> listAllListings(
            @PageableDefault(size = 20, sort = "lastSeenAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.listAllListings(pageable));
    }

    @Operation(summary = "Listar todos os anúncios extraídos de um monitor específico")
    @GetMapping("/{id}/listings")
    public ResponseEntity<Page<ScrapedListingResponse>> listMonitorListings(
            @PathVariable UUID id,
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "tier", required = false) MatchTier tier,
            @RequestParam(value = "deliveryOnly", required = false) Boolean deliveryOnly,
            @PageableDefault(size = 20, sort = "lastSeenAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.listMonitorListings(id, keyword, tier, deliveryOnly, pageable));
    }

    @Operation(summary = "Listar logs de chamadas de IA de um monitor específico")
    @GetMapping("/{id}/ai-logs")
    public ResponseEntity<Page<br.dev.bielsolosos.biscraper.domain.ai.model.dto.AiAnalysisLogResponse>> listMonitorAiLogs(
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(aiAnalysisLogService.listMonitorAiLogs(id, pageable));
    }

    @Operation(summary = "Buscar detalhes de um monitor específico pelo ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProductMonitorResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @Operation(summary = "Desativar monitoramento de um produto")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ProductMonitorResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(service.deactivate(id));
    }

    @Operation(summary = "Reativar monitoramento de um produto")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ProductMonitorResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(service.activate(id));
    }

    @Operation(summary = "Excluir permanentemente um monitor de produtos")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
