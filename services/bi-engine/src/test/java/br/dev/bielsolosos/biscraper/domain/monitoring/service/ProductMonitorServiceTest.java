package br.dev.bielsolosos.biscraper.domain.monitoring.service;

import br.dev.bielsolosos.biscraper.core.abstractfields.SimpleAnalisisTypeFields;
import br.dev.bielsolosos.biscraper.domain.monitoring.mapper.ProductMonitorMapper;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.monitor.ProductMonitorRequest;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.monitor.ProductMonitorResponse;
import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.core.enums.ScrapingFrequency;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import br.dev.bielsolosos.biscraper.core.exception.BusinessException;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ProductMonitorRepository;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.service.MeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductMonitorServiceTest {

    @Mock
    private ProductMonitorRepository repository;

    @Mock
    private ProductMonitorMapper mapper;

    @Mock
    private MeService meService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublishers;

    @InjectMocks
    private ProductMonitorService service;

    private User owner;
    private User otherUser;
    private UUID monitorId;
    private ProductMonitor monitor;
    private ProductMonitorRequest request;
    private ProductMonitorResponse response;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(UUID.randomUUID())
                .username("bielsolosos")
                .email("biel@dev.com")
                .active(true)
                .build();

        otherUser = User.builder()
                .id(UUID.randomUUID())
                .username("other_user")
                .email("other@dev.com")
                .active(true)
                .build();

        monitorId = UUID.randomUUID();

        monitor = ProductMonitor.builder()
                .id(monitorId)
                .user(owner)
                .name("Notebook Gamer")
                .description("Monitoramento de notebooks")
                .analysisType(AnalysisType.SIMPLE)
                .targetVendor(Vendor.OLX)
                .active(true)
                .cronExpression(ScrapingFrequency.DAILY.getCronExpression())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        SimpleAnalisisTypeFields fields = new SimpleAnalisisTypeFields();
        fields.setPrompt("Buscar notebooks i7 com 16gb de ram");

        request = new ProductMonitorRequest(
                "Notebook Gamer",
                "Monitoramento de notebooks",
                Vendor.OLX,
                AnalysisType.SIMPLE,
                fields,
                List.of("notebook gamer", "notebook i7"),
                BigDecimal.valueOf(2000),
                BigDecimal.valueOf(4500),
                "SP",
                "São Paulo",
                true,
                ScrapingFrequency.DAILY
        );

        response = new ProductMonitorResponse(
                monitorId,
                "Notebook Gamer",
                "Monitoramento de notebooks",
                AnalysisType.SIMPLE,
                Vendor.OLX,
                true,
                ScrapingFrequency.DAILY,
                ScrapingFrequency.DAILY.getCronExpression(),
                null,
                Collections.emptyList(),
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    @Test
    @DisplayName("Deve criar um ProductMonitor com sucesso")
    void shouldCreateProductMonitorSuccessfully() {
        when(meService.getMe()).thenReturn(owner);
        when(mapper.toEntity(request, owner)).thenReturn(monitor);
        when(repository.save(monitor)).thenReturn(monitor);
        when(mapper.toResponse(monitor)).thenReturn(response);

        ProductMonitorResponse result = service.create(request);

        assertNotNull(result);
        assertEquals(monitorId, result.id());
        assertEquals("Notebook Gamer", result.name());
        verify(meService, times(1)).getMe();
        verify(repository, times(1)).save(monitor);
    }

    @Test
    @DisplayName("Deve listar monitores paginados para o usuário autenticado")
    void shouldListPagedMonitorsForCurrentUser() {
        when(meService.getMe()).thenReturn(owner);
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductMonitor> page = new PageImpl<>(List.of(monitor), pageable, 1);

        when(repository.findByUserId(owner.getId(), pageable)).thenReturn(page);
        when(mapper.toResponse(monitor)).thenReturn(response);

        Page<ProductMonitorResponse> result = service.listPaged(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(monitorId, result.getContent().get(0).id());
        verify(repository, times(1)).findByUserId(owner.getId(), pageable);
    }

    @Test
    @DisplayName("Deve buscar um monitor por ID com sucesso quando pertencer ao usuário")
    void shouldGetByIdSuccessfully() {
        when(repository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(meService.getMe()).thenReturn(owner);
        when(mapper.toResponse(monitor)).thenReturn(response);

        ProductMonitorResponse result = service.getById(monitorId);

        assertNotNull(result);
        assertEquals(monitorId, result.id());
        verify(repository, times(1)).findById(monitorId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando o monitor não for encontrado pelo ID")
    void shouldThrowExceptionWhenMonitorNotFound() {
        when(repository.findById(monitorId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getById(monitorId));
        assertTrue(ex.getMessage().contains("não encontrado"));
        verify(repository, times(1)).findById(monitorId);
    }

    @Test
    @DisplayName("Deve lançar exceção quando o usuário tentar acessar monitor de outro usuário")
    void shouldThrowExceptionWhenUserHasNoPermission() {
        when(repository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(meService.getMe()).thenReturn(otherUser);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getById(monitorId));
        assertEquals("Você não tem permissão para acessar ou modificar este monitor", ex.getMessage());
    }

    @Test
    @DisplayName("Deve atualizar um monitor existente com sucesso")
    void shouldUpdateMonitorSuccessfully() {
        when(repository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(meService.getMe()).thenReturn(owner);
        doNothing().when(mapper).updateEntity(monitor, request);
        when(repository.save(monitor)).thenReturn(monitor);
        when(mapper.toResponse(monitor)).thenReturn(response);

        ProductMonitorResponse result = service.update(monitorId, request);

        assertNotNull(result);
        verify(mapper, times(1)).updateEntity(monitor, request);
        verify(repository, times(1)).save(monitor);
    }

    @Test
    @DisplayName("Deve desativar um monitor com sucesso")
    void shouldDeactivateMonitorSuccessfully() {
        when(repository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(meService.getMe()).thenReturn(owner);
        when(repository.save(monitor)).thenReturn(monitor);
        when(mapper.toResponse(monitor)).thenReturn(response);

        ProductMonitorResponse result = service.deactivate(monitorId);

        assertNotNull(result);
        assertFalse(monitor.isActive());
        verify(repository, times(1)).save(monitor);
    }

    @Test
    @DisplayName("Deve reativar um monitor com sucesso")
    void shouldActivateMonitorSuccessfully() {
        monitor.setActive(false);
        when(repository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(meService.getMe()).thenReturn(owner);
        when(repository.save(monitor)).thenReturn(monitor);
        when(mapper.toResponse(monitor)).thenReturn(response);

        ProductMonitorResponse result = service.activate(monitorId);

        assertNotNull(result);
        assertTrue(monitor.isActive());
        verify(repository, times(1)).save(monitor);
    }

    @Test
    @DisplayName("Deve deletar um monitor com sucesso")
    void shouldDeleteMonitorSuccessfully() {
        when(repository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(meService.getMe()).thenReturn(owner);

        service.delete(monitorId);

        verify(repository, times(1)).delete(monitor);
    }

    @Test
    @DisplayName("Deve impedir exclusão de monitor por usuário não autorizado")
    void shouldPreventDeleteByUnauthorizedUser() {
        when(repository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(meService.getMe()).thenReturn(otherUser);

        assertThrows(BusinessException.class, () -> service.delete(monitorId));
        verify(repository, never()).delete(any(ProductMonitor.class));
    }
}
