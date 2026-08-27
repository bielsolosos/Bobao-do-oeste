package br.dev.bielsolosos.biscraper.domain.monitoring.service;

import br.dev.bielsolosos.biscraper.domain.monitoring.mapper.ProductMonitorMapper;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.ProductMonitorRequest;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.ProductMonitorResponse;
import br.dev.bielsolosos.biscraper.core.exception.BusinessException;
import br.dev.bielsolosos.biscraper.domain.monitoring.event.MonitorCreatedEvent;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ProductMonitorRepository;
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
