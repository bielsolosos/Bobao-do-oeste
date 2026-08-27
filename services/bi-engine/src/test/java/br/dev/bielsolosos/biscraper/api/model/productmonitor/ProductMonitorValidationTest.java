package br.dev.bielsolosos.biscraper.api.model.productmonitor;

import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.core.enums.ScrapingFrequency;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductMonitorValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Deve validar com sucesso quando todos os campos obrigatórios estiverem corretos")
    void shouldValidateSuccessfullyWhenValid() {
        ProductMonitorRequest validRequest = new ProductMonitorRequest(
                "Monitor Válido",
                "Descrição",
                Vendor.OLX,
                AnalysisType.SIMPLE,
                null,
                List.of("termo de busca"),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(500),
                "SP",
                "Capital",
                true,
                ScrapingFrequency.DAILY
        );

        Set<ConstraintViolation<ProductMonitorRequest>> violations = validator.validate(validRequest);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Deve falhar na validação quando nome for vazio ou em branco")
    void shouldFailWhenNameIsBlank() {
        ProductMonitorRequest invalidRequest = new ProductMonitorRequest(
                "   ",
                "Descrição",
                Vendor.OLX,
                AnalysisType.SIMPLE,
                null,
                List.of("termo"),
                BigDecimal.ZERO,
                BigDecimal.TEN,
                null,
                null,
                false,
                null
        );

        Set<ConstraintViolation<ProductMonitorRequest>> violations = validator.validate(invalidRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    @DisplayName("Deve falhar na validação quando vendor ou analysisType forem nulos")
    void shouldFailWhenRequiredEnumsAreNull() {
        ProductMonitorRequest invalidRequest = new ProductMonitorRequest(
                "Monitor",
                "Descrição",
                null,
                null,
                null,
                List.of("termo"),
                BigDecimal.ZERO,
                BigDecimal.TEN,
                null,
                null,
                false,
                null
        );

        Set<ConstraintViolation<ProductMonitorRequest>> violations = validator.validate(invalidRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("vendor")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("analysisType")));
    }

    @Test
    @DisplayName("Deve falhar na validação quando searchKeywords estiver vazia")
    void shouldFailWhenSearchKeywordsIsEmpty() {
        ProductMonitorRequest invalidRequest = new ProductMonitorRequest(
                "Monitor",
                "Descrição",
                Vendor.OLX,
                AnalysisType.SIMPLE,
                null,
                Collections.emptyList(),
                BigDecimal.ZERO,
                BigDecimal.TEN,
                null,
                null,
                false,
                null
        );

        Set<ConstraintViolation<ProductMonitorRequest>> violations = validator.validate(invalidRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("searchKeywords")));
    }

    @Test
    @DisplayName("Deve falhar na validação quando minPrice ou maxPrice forem negativos")
    void shouldFailWhenPricesAreNegative() {
        ProductMonitorRequest invalidRequest = new ProductMonitorRequest(
                "Monitor",
                "Descrição",
                Vendor.OLX,
                AnalysisType.SIMPLE,
                null,
                List.of("termo"),
                BigDecimal.valueOf(-10),
                BigDecimal.valueOf(-50),
                null,
                null,
                false,
                null
        );

        Set<ConstraintViolation<ProductMonitorRequest>> violations = validator.validate(invalidRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("minPrice")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("maxPrice")));
    }
}
