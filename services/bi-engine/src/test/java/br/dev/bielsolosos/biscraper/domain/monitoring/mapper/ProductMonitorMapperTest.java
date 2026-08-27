package br.dev.bielsolosos.biscraper.domain.monitoring.mapper;

import br.dev.bielsolosos.biscraper.core.abstractfields.NotebookAnalysisTypeFields;
import br.dev.bielsolosos.biscraper.core.abstractfields.SimpleAnalisisTypeFields;
import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.core.enums.ScrapingFrequency;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.ProductMonitorRequest;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.ProductMonitorResponse;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductMonitorMapperTest {

    private ProductMonitorMapper mapper;
    private ObjectMapper objectMapper;
    private User user;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mapper = new ProductMonitorMapper(objectMapper);

        user = User.builder()
                .id(UUID.randomUUID())
                .username("bielsolosos")
                .email("biel@dev.com")
                .build();
    }

    @Test
    @DisplayName("Deve converter ProductMonitorRequest com SimpleAnalisisTypeFields para entidade")
    void shouldMapRequestWithSimpleFieldsToEntity() {
        SimpleAnalisisTypeFields simpleFields = new SimpleAnalisisTypeFields();
        simpleFields.setPrompt("Quero iPhones 13");

        ProductMonitorRequest request = new ProductMonitorRequest(
                "Busca iPhone",
                "Descrição iPhone",
                Vendor.OLX,
                AnalysisType.SIMPLE,
                simpleFields,
                List.of("iphone 13", "iphone 13 pro"),
                BigDecimal.valueOf(2500),
                BigDecimal.valueOf(3500),
                "SP",
                "Capital",
                true,
                ScrapingFrequency.DAILY
        );

        ProductMonitor entity = mapper.toEntity(request, user);

        assertNotNull(entity);
        assertEquals("Busca iPhone", entity.getName());
        assertEquals("Descrição iPhone", entity.getDescription());
        assertEquals(Vendor.OLX, entity.getTargetVendor());
        assertEquals(AnalysisType.SIMPLE, entity.getAnalysisType());
        assertEquals(ScrapingFrequency.DAILY.getCronExpression(), entity.getCronExpression());
        assertEquals(2, entity.getSearchQueries().size());
        assertEquals("iphone 13", entity.getSearchQueries().get(0).getQueryTerm());
        assertEquals(user, entity.getUser());
        assertNotNull(entity.getExpectedSpecs());
    }

    @Test
    @DisplayName("Deve converter ProductMonitorRequest com NotebookAnalysisTypeFields para entidade")
    void shouldMapRequestWithNotebookFieldsToEntity() {
        NotebookAnalysisTypeFields notebookFields = NotebookAnalysisTypeFields.builder()
                .minimumRamGb(16)
                .needsDedicatedGpu(true)
                .requiredProcessor("i7")
                .requiredStorage("512GB SSD")
                .build();

        ProductMonitorRequest request = new ProductMonitorRequest(
                "Notebook i7",
                "Para jogos",
                Vendor.MERCADO_LIVRE,
                AnalysisType.NOTEBOOK,
                notebookFields,
                List.of("notebook rtx"),
                BigDecimal.valueOf(3000),
                BigDecimal.valueOf(6000),
                null,
                null,
                false,
                ScrapingFrequency.HOURLY
        );

        ProductMonitor entity = mapper.toEntity(request, user);

        assertNotNull(entity);
        assertEquals(AnalysisType.NOTEBOOK, entity.getAnalysisType());
        assertEquals(Vendor.MERCADO_LIVRE, entity.getTargetVendor());
        assertEquals(1, entity.getSearchQueries().size());
        assertNotNull(entity.getExpectedSpecs());
    }

    @Test
    @DisplayName("Deve converter entidade para ProductMonitorResponse")
    void shouldMapEntityToResponse() {
        ProductMonitor entity = ProductMonitor.builder()
                .id(UUID.randomUUID())
                .name("Monitor Teste")
                .description("Desc Teste")
                .analysisType(AnalysisType.SIMPLE)
                .targetVendor(Vendor.OLX)
                .active(true)
                .cronExpression(ScrapingFrequency.HOURLY.getCronExpression())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        ProductMonitorResponse response = mapper.toResponse(entity);

        assertNotNull(response);
        assertEquals(entity.getId(), response.id());
        assertEquals("Monitor Teste", response.name());
        assertEquals("Desc Teste", response.description());
        assertEquals(ScrapingFrequency.HOURLY, response.frequency());
        assertTrue(response.active());
    }

    @Test
    @DisplayName("Deve atualizar entidade a partir de novo request")
    void shouldUpdateEntityFromRequest() {
        ProductMonitor entity = ProductMonitor.builder()
                .id(UUID.randomUUID())
                .name("Nome Antigo")
                .description("Desc Antiga")
                .analysisType(AnalysisType.SIMPLE)
                .targetVendor(Vendor.OLX)
                .cronExpression(ScrapingFrequency.DAILY.getCronExpression())
                .build();

        SimpleAnalisisTypeFields fields = new SimpleAnalisisTypeFields();
        fields.setPrompt("Prompt Atualizado");

        ProductMonitorRequest updateRequest = new ProductMonitorRequest(
                "Nome Novo",
                "Desc Nova",
                Vendor.MERCADO_LIVRE,
                AnalysisType.SIMPLE,
                fields,
                List.of("nova busca"),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(500),
                "RJ",
                "Rio",
                false,
                ScrapingFrequency.EVERY_30_MINUTES
        );

        mapper.updateEntity(entity, updateRequest);

        assertEquals("Nome Novo", entity.getName());
        assertEquals("Desc Nova", entity.getDescription());
        assertEquals(Vendor.MERCADO_LIVRE, entity.getTargetVendor());
        assertEquals(ScrapingFrequency.EVERY_30_MINUTES.getCronExpression(), entity.getCronExpression());
        assertEquals(1, entity.getSearchQueries().size());
        assertEquals("nova busca", entity.getSearchQueries().get(0).getQueryTerm());
    }
}
