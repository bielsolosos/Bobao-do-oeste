package br.dev.bielsolosos.biscraper.domain.monitoring.service;

import br.dev.bielsolosos.biscraper.core.exception.BusinessException;
import br.dev.bielsolosos.biscraper.domain.monitoring.event.MonitorCreatedEvent;
import br.dev.bielsolosos.biscraper.domain.monitoring.mapper.ProductMonitorMapper;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.AiAnalysisLog;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapedListing;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.monitor.AiAnalysisLogResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.monitor.ProductMonitorRequest;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.monitor.ProductMonitorResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapedListingResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.AiAnalysisLogRepository;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ProductMonitorRepository;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ScrapedListingRepository;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.service.MeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductMonitorService {

    private final ProductMonitorRepository repository;
    private final ScrapedListingRepository scrapedListingRepository;
    private final AiAnalysisLogRepository aiAnalysisLogRepository;
    private final ProductMonitorMapper mapper;
    private final MeService meService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ProductMonitorResponse create(ProductMonitorRequest request) {
        User me = meService.getMe();
        log.info("Criando novo ProductMonitor '{}' para o usuário '{}'", request.name(), me.getUsername());

        ProductMonitor monitor = mapper.toEntity(request, me);
        ProductMonitor saved = repository.save(monitor);

        // Dispara evento assíncrono que será processado concorrentemente pelo ScrapingJobDispatcher
        eventPublisher.publishEvent(new MonitorCreatedEvent(saved));
        log.debug("Evento MonitorCreatedEvent publicado para o monitor id '{}'", saved.getId());

        return mapper.toResponse(saved);
    }

    @Transactional
    public ProductMonitorResponse update(UUID id, ProductMonitorRequest request) {
        ProductMonitor monitor = findById(id);
        validatePermission(monitor);

        log.info("Atualizando ProductMonitor com id '{}'", id);
        mapper.updateEntity(monitor, request);
        ProductMonitor updated = repository.save(monitor);

        return mapper.toResponse(updated);
    }

    @Transactional(readOnly = true)
    public Page<ProductMonitorResponse> listPaged(Pageable pageable) {
        User me = meService.getMe();
        log.debug("Listando monitores paginados para o usuário '{}'", me.getUsername());
        return repository.findByUserId(me.getId(), pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductMonitorResponse getById(UUID id) {
        ProductMonitor monitor = findById(id);
        validatePermission(monitor);
        return mapper.toResponse(monitor);
    }

    @Transactional
    public ProductMonitorResponse deactivate(UUID id) {
        ProductMonitor monitor = findById(id);
        validatePermission(monitor);

        log.info("Desativando ProductMonitor com id '{}'", id);
        monitor.setActive(false);
        ProductMonitor updated = repository.save(monitor);

        return mapper.toResponse(updated);
    }

    @Transactional
    public ProductMonitorResponse activate(UUID id) {
        ProductMonitor monitor = findById(id);
        validatePermission(monitor);

        log.info("Reativando ProductMonitor com id '{}'", id);
        monitor.setActive(true);
        ProductMonitor updated = repository.save(monitor);

        return mapper.toResponse(updated);
    }

    @Transactional
    public void delete(UUID id) {
        ProductMonitor monitor = findById(id);
        validatePermission(monitor);

        log.info("Deletando ProductMonitor com id '{}'", id);
        repository.delete(monitor);
    }

    @Transactional(readOnly = true)
    public Page<ScrapedListingResponse> listAllListings(Pageable pageable) {
        User me = meService.getMe();
        log.debug("Listando todos os anúncios raspados do usuário '{}'", me.getUsername());
        return scrapedListingRepository.findByProductMonitorUserId(me.getId(), pageable)
                .map(this::toListingResponse);
    }

    @Transactional(readOnly = true)
    public Page<ScrapedListingResponse> listMonitorListings(UUID monitorId, Pageable pageable) {
        ProductMonitor monitor = findById(monitorId);
        validatePermission(monitor);
        log.debug("Listando anúncios do monitor '{}'", monitorId);
        return scrapedListingRepository.findByProductMonitorId(monitorId, pageable)
                .map(this::toListingResponse);
    }

    @Transactional(readOnly = true)
    public Page<AiAnalysisLogResponse> listAllAiLogs(Pageable pageable) {
        User me = meService.getMe();
        log.debug("Listando logs de IA para o usuário '{}'", me.getUsername());
        return aiAnalysisLogRepository.findByProductMonitorUserId(me.getId(), pageable)
                .map(this::toAiLogResponse);
    }

    @Transactional(readOnly = true)
    public Page<AiAnalysisLogResponse> listMonitorAiLogs(UUID monitorId, Pageable pageable) {
        ProductMonitor monitor = findById(monitorId);
        validatePermission(monitor);
        log.debug("Listando logs de IA do monitor '{}'", monitorId);
        return aiAnalysisLogRepository.findByProductMonitorId(monitorId, pageable)
                .map(this::toAiLogResponse);
    }

    private ScrapedListingResponse toListingResponse(ScrapedListing l) {
        return new ScrapedListingResponse(
                l.getId(),
                l.getProductMonitor() != null ? l.getProductMonitor().getId() : null,
                l.getProductMonitor() != null ? l.getProductMonitor().getName() : null,
                l.getVendor(),
                l.getVendorListingId(),
                l.getTitle(),
                l.getUrl(),
                l.getDescription(),
                l.getCurrentPrice(),
                l.getOriginalPrice(),
                l.getState(),
                l.getCity(),
                l.getNeighborhood(),
                l.isHasDelivery(),
                l.getDeliveryType(),
                l.getImages(),
                l.getMatchTier(),
                l.getMatchScore(),
                l.getExtractedSpecs(),
                l.getPublishedAt(),
                l.getFirstSeenAt(),
                l.getLastSeenAt()
        );
    }

    private AiAnalysisLogResponse toAiLogResponse(AiAnalysisLog log) {
        return new AiAnalysisLogResponse(
                log.getId(),
                log.getProductMonitor() != null ? log.getProductMonitor().getId() : null,
                log.getProductMonitor() != null ? log.getProductMonitor().getName() : null,
                log.getScrapingExecution() != null ? log.getScrapingExecution().getId() : null,
                log.getModelName(),
                log.getVendor(),
                log.getItemsCount(),
                log.getSystemPrompt(),
                log.getUserPrompt(),
                log.getRawResponse(),
                log.getStatus(),
                log.getDurationMs(),
                log.getErrorMessage(),
                log.getCreatedAt()
        );
    }

    private ProductMonitor findById(UUID id) {
        return repository.findById(id)
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
