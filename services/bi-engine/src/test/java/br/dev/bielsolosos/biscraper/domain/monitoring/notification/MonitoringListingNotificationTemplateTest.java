package br.dev.bielsolosos.biscraper.domain.monitoring.notification;

import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.core.enums.NotificationChannel;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapedListing;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MonitoringListingNotificationTemplateTest {

    @Test
    @DisplayName("Deve gerar assunto e corpo textual da notificação corretamente")
    void shouldGenerateSubjectAndMessage() {
        ProductMonitor monitor = ProductMonitor.builder()
                .id(UUID.randomUUID())
                .name("Macbook M1")
                .build();

        ScrapedListing listing = ScrapedListing.builder()
                .id(UUID.randomUUID())
                .title("Macbook Air M1 16GB 256GB")
                .vendor(Vendor.OLX)
                .url("https://olx.com.br/item-123")
                .currentPrice(BigDecimal.valueOf(4500))
                .matchTier(MatchTier.HIGH)
                .matchScore(BigDecimal.valueOf(95.0))
                .city("São Paulo")
                .state("SP")
                .build();

        MonitoringListingNotificationTemplate template = new MonitoringListingNotificationTemplate(
                monitor,
                List.of(listing),
                new NotificationChannel[]{NotificationChannel.DISCORD}
        );

        assertArrayEquals(new NotificationChannel[]{NotificationChannel.DISCORD}, template.getChannels());
        assertTrue(template.getSubject().contains("Macbook M1"));
        assertTrue(template.getMessageTemplate().contains("Macbook Air M1 16GB 256GB"));
        assertTrue(template.getMessageTemplate().contains("R$"));
        assertEquals(monitor, template.getMonitor());
        assertEquals(1, template.getListings().size());
    }

    @Test
    @DisplayName("Deve gerar payload completo do Discord com embeds e campos customizados")
    @SuppressWarnings("unchecked")
    void shouldGenerateDiscordPayloadWithEmbeds() {
        ProductMonitor monitor = ProductMonitor.builder()
                .id(UUID.randomUUID())
                .name("Notebook Gamer")
                .build();

        ScrapedListing listing = ScrapedListing.builder()
                .id(UUID.randomUUID())
                .title("Dell G15 RTX 3050")
                .vendor(Vendor.MERCADO_LIVRE)
                .url("https://mercadolivre.com.br/item-456")
                .currentPrice(BigDecimal.valueOf(3800))
                .matchTier(MatchTier.HIGH)
                .matchScore(BigDecimal.valueOf(92.0))
                .neighborhood("Pinheiros")
                .city("São Paulo")
                .state("SP")
                .hasDelivery(true)
                .deliveryType("FULL")
                .images(List.of("https://http2.mlstatic.com/item.jpg"))
                .build();

        MonitoringListingNotificationTemplate template = new MonitoringListingNotificationTemplate(
                monitor,
                List.of(listing)
        );

        Map<String, Object> payload = template.toDiscordPayload();

        assertNotNull(payload);
        assertEquals("BI Scraper", payload.get("username"));
        assertTrue(((String) payload.get("content")).contains("Notebook Gamer"));

        List<Map<String, Object>> embeds = (List<Map<String, Object>>) payload.get("embeds");
        assertNotNull(embeds);
        assertEquals(1, embeds.size());

        Map<String, Object> embed = embeds.get(0);
        assertEquals("Dell G15 RTX 3050", embed.get("title"));
        assertEquals("https://mercadolivre.com.br/item-456", embed.get("url"));
        assertEquals(0x57F287, embed.get("color")); // Verde para HIGH

        List<Map<String, Object>> fields = (List<Map<String, Object>>) embed.get("fields");
        assertNotNull(fields);
        assertTrue(fields.stream().anyMatch(f -> f.get("name").equals("💰 Preço")));
        assertTrue(fields.stream().anyMatch(f -> f.get("name").equals("🏪 Plataforma")));
        assertTrue(fields.stream().anyMatch(f -> f.get("name").equals("🎯 Match")));
        assertTrue(fields.stream().anyMatch(f -> f.get("name").equals("📍 Local")));
        assertTrue(fields.stream().anyMatch(f -> f.get("name").equals("🚚 Entrega")));
    }
}
