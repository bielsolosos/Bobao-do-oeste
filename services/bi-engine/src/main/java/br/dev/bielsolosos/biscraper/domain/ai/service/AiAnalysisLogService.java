package br.dev.bielsolosos.biscraper.domain.ai.service;

import br.dev.bielsolosos.biscraper.api.mapper.ai.AiAnalysisLogMapper;
import br.dev.bielsolosos.biscraper.core.exception.BusinessException;
import br.dev.bielsolosos.biscraper.domain.ai.model.AiAnalysisLog;
import br.dev.bielsolosos.biscraper.domain.ai.model.dto.AiAnalysisLogResponse;
import br.dev.bielsolosos.biscraper.domain.ai.repository.AiAnalysisLogRepository;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapedListing;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ProductMonitorRepository;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ScrapedListingRepository;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.service.MeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAnalysisLogService {

    private final AiAnalysisLogRepository aiAnalysisLogRepository;
    private final ProductMonitorRepository productMonitorRepository;
    private final ScrapedListingRepository scrapedListingRepository;
    private final MeService meService;

    @Transactional(readOnly = true)
    public Page<AiAnalysisLogResponse> listAllAiLogs(Pageable pageable) {
        User me = meService.getMe();
        log.debug("Listando logs de IA para o usuário '{}'", me.getUsername());
        return aiAnalysisLogRepository.findByProductMonitorUserId(me.getId(), pageable)
                .map(AiAnalysisLogMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AiAnalysisLogResponse> listMonitorAiLogs(UUID monitorId, Pageable pageable) {
        ProductMonitor monitor = findById(monitorId);
        validatePermission(monitor);
        log.debug("Listando logs de IA do monitor '{}'", monitorId);
        return aiAnalysisLogRepository.findByProductMonitorId(monitorId, pageable)
                .map(AiAnalysisLogMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AiAnalysisLogResponse> listLogsByListing(UUID listingId, Pageable pageable) {
        ScrapedListing listing = scrapedListingRepository.findById(listingId)
                .orElseThrow(() -> new BusinessException("Anúncio não encontrado com ID: " + listingId));
        
        validatePermission(listing.getProductMonitor());
        
        if (listing.getLastExecution() == null) {
            return Page.empty(pageable);
        }

        log.debug("Listando logs de IA associados ao anúncio '{}' e execução '{}'", listingId, listing.getLastExecution().getId());
        return aiAnalysisLogRepository.findByScrapingExecutionId(listing.getLastExecution().getId(), pageable)
                .map(AiAnalysisLogMapper::toResponse);
    }

    @Transactional
    public void saveLog(br.dev.bielsolosos.biscraper.domain.ai.model.dto.AiAnalysisLogCreateDto dto) {
        try {
            AiAnalysisLog aiLog = AiAnalysisLog.builder()
                    .productMonitor(dto.productMonitor())
                    .scrapingExecution(dto.scrapingExecution())
                    .modelName(dto.modelName())
                    .vendor(dto.vendor() != null ? dto.vendor() : "GEMINI")
                    .itemsCount(dto.itemsCount())
                    .systemPrompt(dto.systemPrompt())
                    .userPrompt(dto.userPrompt())
                    .rawResponse(dto.rawResponse())
                    .status(dto.status())
                    .durationMs(dto.durationMs())
                    .promptTokens(dto.promptTokens())
                    .generationTokens(dto.generationTokens())
                    .totalTokens(dto.totalTokens())
                    .errorMessage(dto.errorMessage())
                    .build();

            aiAnalysisLogRepository.save(aiLog);
        } catch (Exception ex) {
            log.warn("Falha ao salvar AiAnalysisLog no banco: {}", ex.getMessage());
        }
    }

    private ProductMonitor findById(UUID id) {
        return productMonitorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(String.format("Monitor de produto não encontrado com o ID: %s", id)));
    }

    private void validatePermission(ProductMonitor monitor) {
        User me = meService.getMe();
        if (!monitor.getUser().getId().equals(me.getId())) {
            log.warn("Acesso negado: Usuário '{}' tentou manipular o monitor '{}' de outro usuário", me.getUsername(), monitor.getId());
            throw new BusinessException("Você não tem permissão para acessar ou modificar este monitor");
        }
    }
}
