package br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto;

import br.dev.bielsolosos.biscraper.core.enums.DeliveryType;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ScrapeDetailResponseDeserializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Deve deserializar com sucesso o payload JSON de detalhe do anúncio retornado pelo Scraper")
    void shouldDeserializeScrapeDetailResponseSuccessfully() throws Exception {
        String json = """
                {
                    "success": true,
                    "from_cache": false,
                    "used_fallback": false,
                    "data": {
                        "vendor": "OLX",
                        "vendor_listing_id": "1518847740",
                        "url": "https://df.olx.com.br/distrito-federal-e-regiao/informatica/notebooks/macbook-air-512gb-16gb-m5-13p-midnight-1518847740",
                        "title": "MACBOOK AIR 512GB 16GB M5 13P MIDNIGHT",
                        "price": 0.0,
                        "original_price": null,
                        "description": "JÚLIO IMPORT, WHATSAPP NA DESCRIÇÃO...",
                        "state": "DF",
                        "city": null,
                        "neighborhood": null,
                        "has_delivery": false,
                        "delivery_type": "UNKNOWN",
                        "properties": {
                            "Categoria": "Notebooks",
                            "Marca": "Apple",
                            "Condição": "Novo",
                            "Marca do Processador": "Apple",
                            "Memória RAM": "16 Gb",
                            "Marca da Placa de Vídeo": "Outros",
                            "Tamanho de Tela": "13 Polegadas",
                            "Armazenamento": "512 Gb",
                            "Tipo": "Notebook",
                            "Aceita trocas": "Não",
                            "Para Doação": "Sim"
                        },
                        "images": [
                            "https://img.olx.com.br/images/81/811665546481395.jpg"
                        ],
                        "cached_images": [
                            {
                                "id": "img-01",
                                "image_index": 0,
                                "original_url": "https://img.olx.com.br/images/81/811665546481395.jpg",
                                "endpoint_url": "/api/v1/scrape/images/img-01",
                                "full_endpoint_url": "http://localhost:8000/api/v1/scrape/images/img-01",
                                "mime_type": "image/jpeg",
                                "size_bytes": 10240,
                                "expires_at": "2026-09-03T14:05:08.411235Z"
                            }
                        ],
                        "seller_name": null,
                        "seller_info": {},
                        "published_at": null,
                        "scraped_at": "2026-09-02T14:05:08.411235Z"
                    },
                    "error_message": null
                }
                """;

        ScrapeDetailResponse response = objectMapper.readValue(json, ScrapeDetailResponse.class);

        assertNotNull(response);
        assertTrue(response.success());
        assertFalse(response.fromCache());
        assertFalse(response.usedFallback());
        assertNull(response.errorMessage());

        ScrapeDetailResponse.DetailDto data = response.data();
        assertNotNull(data);
        assertEquals(Vendor.OLX, data.vendor());
        assertEquals("1518847740", data.vendorListingId());
        assertEquals("https://df.olx.com.br/distrito-federal-e-regiao/informatica/notebooks/macbook-air-512gb-16gb-m5-13p-midnight-1518847740", data.url());
        assertEquals("MACBOOK AIR 512GB 16GB M5 13P MIDNIGHT", data.title());
        assertEquals(BigDecimal.ZERO.setScale(1), data.price());
        assertNull(data.originalPrice());
        assertEquals("JÚLIO IMPORT, WHATSAPP NA DESCRIÇÃO...", data.description());
        assertEquals("DF", data.state());
        assertNull(data.city());
        assertNull(data.neighborhood());
        assertFalse(data.hasDelivery());
        assertEquals(DeliveryType.UNKNOWN, data.deliveryType());

        assertNotNull(data.properties());
        assertEquals("Notebooks", data.properties().get("Categoria"));
        assertEquals("Apple", data.properties().get("Marca"));
        assertEquals("16 Gb", data.properties().get("Memória RAM"));
        assertEquals("512 Gb", data.properties().get("Armazenamento"));

        assertNotNull(data.images());
        assertEquals(1, data.images().size());
        assertEquals("https://img.olx.com.br/images/81/811665546481395.jpg", data.images().getFirst());

        assertNotNull(data.cachedImages());
        assertEquals(1, data.cachedImages().size());
        ScrapeDetailResponse.CachedImageDto cachedImage = data.cachedImages().getFirst();
        assertEquals("img-01", cachedImage.id());
        assertEquals(0, cachedImage.imageIndex());
        assertEquals("image/jpeg", cachedImage.mimeType());
        assertEquals(10240L, cachedImage.sizeBytes());
        assertNotNull(cachedImage.expiresAt());

        assertNotNull(data.scrapedAt());
    }

    @Test
    @DisplayName("Deve deserializar payload de erro quando scraping de detalhes falhar")
    void shouldDeserializeErrorResponse() throws Exception {
        String errorJson = """
                {
                    "success": false,
                    "from_cache": false,
                    "used_fallback": true,
                    "data": null,
                    "error_message": "Não foi possível extrair os dados da página do anúncio."
                }
                """;

        ScrapeDetailResponse response = objectMapper.readValue(errorJson, ScrapeDetailResponse.class);

        assertNotNull(response);
        assertFalse(response.success());
        assertFalse(response.fromCache());
        assertTrue(response.usedFallback());
        assertNull(response.data());
        assertEquals("Não foi possível extrair os dados da página do anúncio.", response.errorMessage());
    }
}
