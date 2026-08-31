package br.dev.bielsolosos.biscraper.domain.ai.service;

import br.dev.bielsolosos.biscraper.api.mapper.ai.AiAnalysisLogMapper;
import br.dev.bielsolosos.biscraper.core.exception.BusinessException;
import br.dev.bielsolosos.biscraper.domain.ai.model.AiAnalysisLog;
import br.dev.bielsolosos.biscraper.domain.ai.model.dto.AiAnalysisLogResponse;
import br.dev.bielsolosos.biscraper.domain.ai.repository.AiAnalysisLogRepository;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ProductMonitorRepository;
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
