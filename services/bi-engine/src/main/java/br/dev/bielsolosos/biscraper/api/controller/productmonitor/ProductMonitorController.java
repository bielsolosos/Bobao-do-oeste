package br.dev.bielsolosos.biscraper.api.controller.productmonitor;

import br.dev.bielsolosos.biscraper.api.model.productmonitor.ProductMonitorRequest;
import br.dev.bielsolosos.biscraper.api.model.productmonitor.ProductMonitorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Product Monitor", description = "Endpoints para gerenciar monitoramentos de produtos.")
@RestController
@RequestMapping("/api/v1/product-monitors")
@RequiredArgsConstructor
public class ProductMonitorController {

    @Operation(summary = "Criar um novo monitor de produtos com filtros e parâmetros de IA")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductMonitorResponse create(@Valid @RequestBody ProductMonitorRequest request) {
        // Será conectado ao Service
        return null;
    }
}
