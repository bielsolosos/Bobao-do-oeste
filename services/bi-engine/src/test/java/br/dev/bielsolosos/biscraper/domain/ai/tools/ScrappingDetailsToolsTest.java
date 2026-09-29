package br.dev.bielsolosos.biscraper.domain.ai.tools;

import br.dev.bielsolosos.biscraper.core.enums.DeliveryType;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.ScraperHttpClient;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto.ScrapeDetailRequest;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto.ScrapeDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScrappingDetailsToolsTest {

    @Mock
    private ScraperHttpClient scraperHttpClient;

    @InjectMocks
    private ScrappingDetailsTools tools;

    @Test
    @DisplayName("Deve extrair detalhes adicionais com sucesso quando o Scraper responder com dados válidos")
    void shouldReturnAdditionalInfoSuccessfully() {
        String url = "https://df.olx.com.br/item-12345";
        Vendor vendor = Vendor.OLX;

        Map<String, Object> properties = Map.of(
                "Memória RAM", "16 Gb",
                "Armazenamento", "512 Gb",
                "Processador", "M5"
        );

        ScrapeDetailResponse.DetailDto detailDto = new ScrapeDetailResponse.DetailDto(
                vendor,
                "12345",
                url,
                "Macbook Air M5",
                BigDecimal.valueOf(8000),
                null,
                "Descrição detalhada do MacBook em excelente estado.",
                "DF",
                "Brasília",
                "Asa Sul",
                true,
                DeliveryType.OLX_PAY,
                properties,
                List.of("https://img.olx.com.br/img1.jpg"),
                Collections.emptyList(),
                "Vendedor Teste",
                Collections.emptyMap(),
                Instant.now(),
                Instant.now()
        );

        ScrapeDetailResponse mockResponse = new ScrapeDetailResponse(
                true,
                false,
                false,
                detailDto,
                null
        );

        when(scraperHttpClient.scrapeDetail(any(ScrapeDetailRequest.class))).thenReturn(mockResponse);

        ScrappingDetailsTools.ListingDetailsDto result = tools.getAdditionalInfo(vendor, url);

        assertNotNull(result);
        assertEquals("Macbook Air M5", result.title());
        assertEquals("Descrição detalhada do MacBook em excelente estado.", result.description());
        assertEquals(3, result.properties().size());
        assertEquals("16 Gb", result.properties().get("Memória RAM"));
        assertEquals("512 Gb", result.properties().get("Armazenamento"));

        verify(scraperHttpClient, times(1)).scrapeDetail(any(ScrapeDetailRequest.class));
    }

    @Test
    @DisplayName("Deve retornar objeto vazio quando o Scraper responder success = false ou data = null")
    void shouldReturnEmptyDtoWhenScraperFails() {
        String url = "https://df.olx.com.br/item-inexistente";
        Vendor vendor = Vendor.OLX;

        ScrapeDetailResponse mockResponse = new ScrapeDetailResponse(
                false,
                false,
                true,
                null,
                "Anúncio não encontrado"
        );

        when(scraperHttpClient.scrapeDetail(any(ScrapeDetailRequest.class))).thenReturn(mockResponse);

        ScrappingDetailsTools.ListingDetailsDto result = tools.getAdditionalInfo(vendor, url);

        assertNotNull(result);
        assertNull(result.title());
        assertNull(result.description());
        assertTrue(result.properties().isEmpty());
    }

    @Test
    @DisplayName("Deve tratar exceções do ScraperHttpClient e retornar DTO seguro sem quebrar o fluxo")
    void shouldHandleHttpClientExceptionGracefully() {
        String url = "https://df.olx.com.br/item-com-erro";
        Vendor vendor = Vendor.OLX;

        when(scraperHttpClient.scrapeDetail(any(ScrapeDetailRequest.class)))
                .thenThrow(new RuntimeException("Connection timed out"));

        ScrappingDetailsTools.ListingDetailsDto result = tools.getAdditionalInfo(vendor, url);

        assertNotNull(result);
        assertNull(result.title());
        assertNull(result.description());
        assertTrue(result.properties().isEmpty());
    }

    @Test
    @DisplayName("Deve extrair detalhes adicionais com fotos em paralelo com sucesso quando o Scraper responder com dados válidos")
    void shouldReturnAdditionalInfoAndImagesSuccessfully() {
        String url = "https://df.olx.com.br/item-12345";
        Vendor vendor = Vendor.OLX;

        Map<String, Object> properties = Map.of(
                "Memória RAM", "16 Gb",
                "Processador", "M5"
        );

        ScrapeDetailResponse.CachedImageDto cachedImage = new ScrapeDetailResponse.CachedImageDto(
                "img-1",
                0,
                "https://img.olx.com.br/foto1.jpg",
                "/api/v1/images/img-1.jpg",
                "http://scraper:8000/api/v1/images/img-1.jpg",
                "image/jpeg",
                1024L,
                Instant.now()
        );

        ScrapeDetailResponse.DetailDto detailDto = new ScrapeDetailResponse.DetailDto(
                vendor,
                "12345",
                url,
                "Macbook Air M5",
                BigDecimal.valueOf(8000),
                null,
                "Descrição detalhada com fotos.",
                "DF",
                "Brasília",
                "Asa Sul",
                true,
                DeliveryType.OLX_PAY,
                properties,
                List.of("https://img.olx.com.br/foto1.jpg"),
                List.of(cachedImage),
                "Vendedor Teste",
                Collections.emptyMap(),
                Instant.now(),
                Instant.now()
        );

        ScrapeDetailResponse mockResponse = new ScrapeDetailResponse(
                true,
                false,
                false,
                detailDto,
                null
        );

        byte[] fakeImageBytes = new byte[]{1, 2, 3, 4};
        when(scraperHttpClient.scrapeDetail(any(ScrapeDetailRequest.class))).thenReturn(mockResponse);
        when(scraperHttpClient.scrapeImageDetails("/api/v1/images/img-1.jpg")).thenReturn(fakeImageBytes);

        ScrappingDetailsTools.ListingDetailsWithImagesDto result = tools.getAdditionalInfoAndImages(vendor, url);

        assertNotNull(result);
        assertEquals(url, result.originalUrl());
        assertEquals("Macbook Air M5", result.title());
        assertEquals("Descrição detalhada com fotos.", result.description());
        assertEquals(2, result.properties().size());
        assertEquals(1, result.images().size());
        assertArrayEquals(fakeImageBytes, result.images().get("https://img.olx.com.br/foto1.jpg"));

        verify(scraperHttpClient, times(1)).scrapeDetail(any(ScrapeDetailRequest.class));
        verify(scraperHttpClient, times(1)).scrapeImageDetails("/api/v1/images/img-1.jpg");
    }

    @Test
    @DisplayName("Deve retornar objeto vazio para fotos quando o Scraper responder success = false ou data = null")
    void shouldReturnEmptyDtoWhenScraperFailsForImages() {
        String url = "https://df.olx.com.br/item-inexistente";
        Vendor vendor = Vendor.OLX;

        ScrapeDetailResponse mockResponse = new ScrapeDetailResponse(
                false,
                false,
                true,
                null,
                "Anúncio não encontrado"
        );

        when(scraperHttpClient.scrapeDetail(any(ScrapeDetailRequest.class))).thenReturn(mockResponse);

        ScrappingDetailsTools.ListingDetailsWithImagesDto result = tools.getAdditionalInfoAndImages(vendor, url);

        assertNotNull(result);
        assertNull(result.title());
        assertNull(result.description());
        assertTrue(result.images().isEmpty());
        assertTrue(result.properties().isEmpty());
    }

    @Test
    @DisplayName("Deve tratar exceções do ScraperHttpClient em getAdditionalInfoAndImages e retornar DTO seguro")
    void shouldHandleHttpClientExceptionGracefullyForImages() {
        String url = "https://df.olx.com.br/item-com-erro";
        Vendor vendor = Vendor.OLX;

        when(scraperHttpClient.scrapeDetail(any(ScrapeDetailRequest.class)))
                .thenThrow(new RuntimeException("Connection timed out"));

        ScrappingDetailsTools.ListingDetailsWithImagesDto result = tools.getAdditionalInfoAndImages(vendor, url);

        assertNotNull(result);
        assertNull(result.title());
        assertNull(result.description());
        assertTrue(result.images().isEmpty());
        assertTrue(result.properties().isEmpty());
    }
}
