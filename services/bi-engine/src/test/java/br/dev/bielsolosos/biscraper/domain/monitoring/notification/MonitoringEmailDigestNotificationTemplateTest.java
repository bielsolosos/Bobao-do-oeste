package br.dev.bielsolosos.biscraper.domain.monitoring.notification;

import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.core.enums.NotificationChannel;
import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapedListing;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MonitoringEmailDigestNotificationTemplateTest {

    @Test
    @DisplayName("Deve gerar assunto, texto e HTML do e-mail de digest corretamente")
    void shouldGenerateEmailDigestContent() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .username("bielsolosos")
                .email("biel@dev.com")
                .build();

        ProductMonitor monitor = ProductMonitor.builder()
                .id(UUID.randomUUID())
                .name("Macbook M2")
                .build();

        ScrapedListing listing1 = ScrapedListing.builder()
                .id(UUID.randomUUID())
                .productMonitor(monitor)
                .title("Macbook Air M2 8GB 256GB Cinza Espacial")
                .vendor(Vendor.OLX)
                .url("https://olx.com.br/item1")
                .currentPrice(BigDecimal.valueOf(4200))
                .matchTier(MatchTier.HIGH)
                .matchScore(BigDecimal.valueOf(96.0))
                .city("São Paulo")
                .state("SP")
                .hasDelivery(true)
                .images(List.of("https://img.olx.com.br/item1.jpg"))
                .build();

        ScrapedListing listing2 = ScrapedListing.builder()
                .id(UUID.randomUUID())
                .productMonitor(monitor)
                .title("Macbook Pro M2 16GB 512GB")
                .vendor(Vendor.MERCADO_LIVRE)
                .url("https://mercadolivre.com.br/item2")
                .currentPrice(BigDecimal.valueOf(6500))
                .matchTier(MatchTier.HIGH)
                .matchScore(BigDecimal.valueOf(98.5))
                .city("Campinas")
                .state("SP")
                .hasDelivery(false)
                .build();

        MonitoringEmailDigestNotificationTemplate template =
                new MonitoringEmailDigestNotificationTemplate(user, List.of(listing1, listing2), 4);

        assertArrayEquals(new NotificationChannel[]{NotificationChannel.EMAIL}, template.getChannels());
        assertTrue(template.getSubject().contains("2 novas oportunidades"));
        assertTrue(template.getSubject().contains("4h"));

        String text = template.getMessageTemplate();
        assertTrue(text.contains("Macbook Air M2"));
        assertTrue(text.contains("Macbook Pro M2"));
        assertTrue(text.contains("[Monitor: Macbook M2]"));
        assertTrue(text.contains("4.200,00"));

        String html = template.toHtmlEmail();
        assertNotNull(html);
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("biel@dev.com"));
        assertTrue(html.contains("Macbook Air M2 8GB 256GB Cinza Espacial"));
        assertTrue(html.contains("Monitor: Macbook M2"));
        assertTrue(html.contains("https://bi.bielsolosos.dev.br/monitors"));
        assertTrue(html.contains("https://olx.com.br/item1"));
        assertTrue(html.contains("https://img.olx.com.br/item1.jpg"));
        assertTrue(html.contains("MATCH 96.0%"));
        assertTrue(html.contains("4.200,00"));
    }

    @Test
    @DisplayName("Deve limitar exibição de itens ao maxItems configurado e exibir aviso de itens restantes")
    void shouldLimitItemsToMaxConfigured() {
        User user = User.builder().id(UUID.randomUUID()).email("biel@dev.com").build();
        ProductMonitor monitor = ProductMonitor.builder().id(UUID.randomUUID()).name("Monitor Teste").build();

        List<ScrapedListing> sixListings = List.of(
                ScrapedListing.builder().id(UUID.randomUUID()).productMonitor(monitor).title("Item 1").currentPrice(BigDecimal.TEN).build(),
                ScrapedListing.builder().id(UUID.randomUUID()).productMonitor(monitor).title("Item 2").currentPrice(BigDecimal.TEN).build(),
                ScrapedListing.builder().id(UUID.randomUUID()).productMonitor(monitor).title("Item 3").currentPrice(BigDecimal.TEN).build(),
                ScrapedListing.builder().id(UUID.randomUUID()).productMonitor(monitor).title("Item 4").currentPrice(BigDecimal.TEN).build(),
                ScrapedListing.builder().id(UUID.randomUUID()).productMonitor(monitor).title("Item 5").currentPrice(BigDecimal.TEN).build(),
                ScrapedListing.builder().id(UUID.randomUUID()).productMonitor(monitor).title("Item 6").currentPrice(BigDecimal.TEN).build()
        );

        MonitoringEmailDigestNotificationTemplate template =
                new MonitoringEmailDigestNotificationTemplate(user, sixListings, 4, "https://app.dev", 4);

        String html = template.toHtmlEmail();
        assertTrue(html.contains("Item 1"));
        assertTrue(html.contains("Item 4"));
        assertFalse(html.contains("Item 5"));
        assertFalse(html.contains("Item 6"));
        assertTrue(html.contains("E mais <strong>2 oportunidades</strong>"));
        assertTrue(html.contains("https://app.dev/monitors"));

        String text = template.getMessageTemplate();
        assertTrue(text.contains("Item 1"));
        assertTrue(text.contains("... e mais 2 oportunidades encontradas!"));
    }
}
