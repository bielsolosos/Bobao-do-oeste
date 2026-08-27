package br.dev.bielsolosos.biscraper.domain.monitoring.service;

import br.dev.bielsolosos.biscraper.api.mapper.productmonitor.ProductMonitorMapper;
import br.dev.bielsolosos.biscraper.api.model.productmonitor.ProductMonitorRequest;
import br.dev.bielsolosos.biscraper.api.model.productmonitor.ProductMonitorResponse;
import br.dev.bielsolosos.biscraper.core.exception.BusinessException;
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
public class ProductMonitorService {

    private final ProductMonitorRepository repository;
    private final ProductMonitorMapper mapper;
    private final MeService meService;

    @Transactional
    public ProductMonitorResponse create(ProductMonitorRequest request) {
        User me = meService.getMe();
        log.info("Criando novo ProductMonitor '{}' para o usuário '{}'", request.name(), me.getUsername());

        ProductMonitor monitor = mapper.toEntity(request, me);
        ProductMonitor saved = repository.save(monitor);

        // TODO: Iniciar a lógica de agendamento e disparo assíncrono dos scrapings.
        // Esse evento assíncrono notificará o serviço de mensageria/agendador para despachar
        // o payload inicial para o Scraper Python ou enfileirar no SQLite/RabbitMQ.
        log.debug("ProductMonitor criado com id '{}'. Disparo de scraping assíncrono pendente.", saved.getId());

        return mapper.toResponse(saved);
    }

    @Transactional
    public ProductMonitorResponse update(UUID id, ProductMonitorRequest request) {
        ProductMonitor monitor = findById(id);
        validatePermission(monitor);

        log.info("Atualizando ProductMonitor com id '{}'", id);
        mapper.updateEntity(monitor, request);
        ProductMonitor updated = repository.save(monitor);

        // TODO: Publicar evento assíncrono para atualizar os parâmetros de busca e agendamentos no Scraper Python.

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

        // TODO: Publicar evento assíncrono para cancelar/pausar os agendamentos ativos no Scraper Python.

        return mapper.toResponse(updated);
    }

    @Transactional
    public ProductMonitorResponse activate(UUID id) {
        ProductMonitor monitor = findById(id);
        validatePermission(monitor);

        log.info("Reativando ProductMonitor com id '{}'", id);
        monitor.setActive(true);
        ProductMonitor updated = repository.save(monitor);

        // TODO: Publicar evento assíncrono para reativar o agendamento de scraping no Scraper Python.

        return mapper.toResponse(updated);
    }

    @Transactional
    public void delete(UUID id) {
        ProductMonitor monitor = findById(id);
        validatePermission(monitor);

        log.info("Deletando ProductMonitor com id '{}'", id);
        // TODO: Publicar evento assíncrono para remover jobs agendados e limpar referências no scraper se necessário.
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
