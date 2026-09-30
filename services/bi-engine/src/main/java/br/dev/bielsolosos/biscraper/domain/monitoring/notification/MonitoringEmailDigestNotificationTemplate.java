package br.dev.bielsolosos.biscraper.domain.monitoring.notification;

import br.dev.bielsolosos.biscraper.core.enums.NotificationChannel;
import br.dev.bielsolosos.biscraper.core.utils.NotificationTemplate;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapedListing;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Getter
public class MonitoringEmailDigestNotificationTemplate implements NotificationTemplate {

    private final User recipient;
    private final List<ScrapedListing> listings;
    private final int windowHours;
    private final String appUrl;
    private final int maxItems;

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final String DEFAULT_APP_URL = "https://bi.bielsolosos.dev.br";
    private static final int DEFAULT_MAX_ITEMS = 4;

    public MonitoringEmailDigestNotificationTemplate(User recipient, List<ScrapedListing> listings, int windowHours, String appUrl, int maxItems) {
        this.recipient = recipient;
        this.listings = listings != null ? listings : Collections.emptyList();
        this.windowHours = windowHours > 0 ? windowHours : 4;
        this.appUrl = StringUtils.isNotBlank(appUrl) ? appUrl.replaceAll("/+$", "") : DEFAULT_APP_URL;
        this.maxItems = maxItems > 0 ? maxItems : DEFAULT_MAX_ITEMS;
    }

    public MonitoringEmailDigestNotificationTemplate(User recipient, List<ScrapedListing> listings, int windowHours) {
        this(recipient, listings, windowHours, DEFAULT_APP_URL, DEFAULT_MAX_ITEMS);
    }

    @Override
    public NotificationChannel[] getChannels() {
        return new NotificationChannel[]{NotificationChannel.EMAIL};
    }

    @Override
    public String getSubject() {
        int count = listings.size();
        return String.format("[BI Scraper] %d %s HIGH match %s nas últimas %dh",
                count,
                count == 1 ? "nova oportunidade" : "novas oportunidades",
                count == 1 ? "encontrada" : "encontradas",
                windowHours);
    }

    @Override
    public String getMessageTemplate() {
        if (listings.isEmpty()) {
            return "Nenhuma oportunidade HIGH match encontrada no período recente.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Resumo BI Scraper — %d oportunidades encontradas nas últimas %d horas:\n\n",
                listings.size(), windowHours));

        List<ScrapedListing> displayed = listings.stream().limit(maxItems).toList();

        for (ScrapedListing listing : displayed) {
            String monitorName = "Geral";
            if (listing.getProductMonitor() != null) {
                try {
                    if (StringUtils.isNotBlank(listing.getProductMonitor().getName())) {
                        monitorName = listing.getProductMonitor().getName();
                    }
                } catch (Exception ignored) {
                }
            }
            String price = listing.getCurrentPrice() != null
                    ? NumberFormat.getCurrencyInstance(PT_BR).format(listing.getCurrentPrice())
                    : "N/A";
            String score = listing.getMatchScore() != null ? listing.getMatchScore().toPlainString() + "%" : "0%";

            sb.append(String.format("• [Monitor: %s] %s\n  Preço: %s | Match: %s (%s) | Plataforma: %s\n  Link: %s\n\n",
                    monitorName,
                    listing.getTitle(),
                    price,
                    listing.getMatchTier() != null ? listing.getMatchTier().name() : "HIGH",
                    score,
                    listing.getVendor() != null ? listing.getVendor().name() : "N/A",
                    listing.getUrl()));
        }

        if (listings.size() > maxItems) {
            sb.append(String.format("... e mais %d oportunidades encontradas! Acesse %s/monitors para ver todas.\n",
                    listings.size() - maxItems, appUrl));
        } else {
            sb.append(String.format("Acesse seu painel em: %s/monitors\n", appUrl));
        }

        return sb.toString();
    }

    @Override
    public String toHtmlEmail() {
        String nowStr = OffsetDateTime.now().format(DATE_FORMATTER);
        StringBuilder cardsHtml = new StringBuilder();

        List<ScrapedListing> displayed = listings.stream().limit(maxItems).toList();
        for (ScrapedListing listing : displayed) {
            cardsHtml.append(buildListingCardHtml(listing));
        }

        int remaining = Math.max(0, listings.size() - maxItems);
        String remainingNoticeHtml = remaining > 0
                ? String.format("""
                    <div style="margin-top: 12px; background-color: #FEF3C7; border: 1px solid #FCD34D; border-radius: 8px; padding: 12px 16px; color: #92400E; font-size: 13px; font-weight: 600; text-align: center;">
                      + E mais <strong>%d oportunidades</strong> encontradas neste ciclo de monitoramento.
                    </div>
                    """, remaining)
                : "";

        return String.format("""
            <!DOCTYPE html>
            <html lang="pt-BR">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>%s</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #F8FAFC; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #0F172A;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color: #F8FAFC; padding: 32px 16px;">
                <tr>
                  <td align="center">
                    <table width="600" cellpadding="0" cellspacing="0" style="max-width: 600px; width: 100%%; background-color: #FFFFFF; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.06); border: 1px solid #E2E8F0;">
                      <!-- HEADER -->
                      <tr>
                        <td style="background: linear-gradient(135deg, #0F172A 0%%, #1E293B 100%%); padding: 36px 32px; text-align: left; border-bottom: 3px solid #F59E0B;">
                          <table cellpadding="0" cellspacing="0" style="margin-bottom: 8px;">
                            <tr>
                              <td style="vertical-align: middle; padding-right: 8px;">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#F59E0B" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="display: block;"><path d="m12 3-1.912 5.813a2 2 0 0 1-1.275 1.275L3 12l5.813 1.912a2 2 0 0 1 1.275 1.275L12 21l1.912-5.813a2 2 0 0 1 1.275-1.275L21 12l-5.813-1.912a2 2 0 0 1-1.275-1.275L12 3Z"/></svg>
                              </td>
                              <td style="vertical-align: middle;">
                                <span style="color: #F59E0B; font-size: 11px; font-weight: 700; letter-spacing: 2px; text-transform: uppercase;">BI Scraper • Alerta de Oportunidades</span>
                              </td>
                            </tr>
                          </table>
                          <h1 style="margin: 0; color: #FFFFFF; font-size: 22px; font-weight: 700; line-height: 1.3;">Resumo Periódico de Garimpo</h1>
                          <p style="margin: 8px 0 0 0; color: #94A3B8; font-size: 13px;">Encontramos <strong>%d oportunidades de alta relevância (HIGH match)</strong> nas últimas %d horas.</p>
                        </td>
                      </tr>

                      <!-- INFO BAR -->
                      <tr>
                        <td style="background-color: #F1F5F9; padding: 12px 32px; font-size: 12px; color: #64748B; border-bottom: 1px solid #E2E8F0;">
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#64748B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align: -2px; margin-right: 4px; display: inline-block;"><rect width="18" height="18" x="3" y="4" rx="2" ry="2"/><line x1="16" x2="16" y1="2" y2="6"/><line x1="8" x2="8" y1="2" y2="6"/><line x1="3" x2="21" y1="10" y2="10"/></svg>
                          Gerado em %s &bull; Destinatário: <strong>%s</strong>
                        </td>
                      </tr>

                      <!-- CARDS LIST -->
                      <tr>
                        <td style="padding: 24px 32px;">
                          %s
                          %s

                          <!-- CTA BUTTON TO APP -->
                          <div style="margin-top: 28px; text-align: center; padding-top: 20px; border-top: 1px dashed #CBD5E1;">
                            <a href="%s/monitors" target="_blank" style="display: inline-block; background: linear-gradient(135deg, #0F172A 0%%, #2563EB 100%%); color: #FFFFFF; text-decoration: none; padding: 14px 28px; border-radius: 10px; font-size: 14px; font-weight: 700; box-shadow: 0 4px 14px rgba(37, 99, 235, 0.25);">
                              Acessar Painel de Monitores no BI Scraper &rarr;
                            </a>
                            <p style="margin: 8px 0 0 0; color: #94A3B8; font-size: 11px;">Veja métricas, histórico de preços e todos os anúncios capturados.</p>
                          </div>
                        </td>
                      </tr>

                      <!-- FOOTER -->
                      <tr>
                        <td style="background-color: #F8FAFC; padding: 24px 32px; text-align: center; border-top: 1px solid #E2E8F0; font-size: 12px; color: #64748B; line-height: 1.6;">
                          <p style="margin: 0 0 4px 0; font-weight: 600; color: #334155;">BI Scraper Intelligence</p>
                          <p style="margin: 0;">Você recebeu este e-mail porque ativou as notificações periódicas no seu painel de configurações.</p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """,
                getSubject(),
                listings.size(),
                windowHours,
                nowStr,
                recipient != null ? recipient.getEmail() : "Usuário",
                cardsHtml.toString(),
                remainingNoticeHtml,
                appUrl);
    }

    private String buildListingCardHtml(ScrapedListing listing) {
        String priceFormatted = listing.getCurrentPrice() != null
                ? NumberFormat.getCurrencyInstance(PT_BR).format(listing.getCurrentPrice())
                : "Preço sob consulta";

        String matchScoreFormatted = listing.getMatchScore() != null
                ? listing.getMatchScore().toPlainString() + "%"
                : "90%+";

        String platform = listing.getVendor() != null ? listing.getVendor().name() : "MARKETPLACE";
        String location = formatLocation(listing);

        ProductMonitor monitor = listing.getProductMonitor();
        String monitorName = "Geral";
        String monitorDetailUrl = String.format("%s/monitors", appUrl);
        if (monitor != null) {
            try {
                if (StringUtils.isNotBlank(monitor.getName())) {
                    monitorName = monitor.getName();
                }
                if (monitor.getId() != null) {
                    monitorDetailUrl = String.format("%s/monitors/%s", appUrl, monitor.getId());
                }
            } catch (Exception ignored) {
                // Fallback gracioso caso proxy esteja desanexado sem sessão ativa
            }
        }

        String thumbnailHtml = "";
        if (listing.getImages() != null && !listing.getImages().isEmpty()) {
            String imgUrl = listing.getImages().get(0);
            if (StringUtils.isNotBlank(imgUrl) && imgUrl.startsWith("http")) {
                thumbnailHtml = String.format("""
                    <td width="90" style="vertical-align: top; padding-right: 16px;">
                      <img src="%s" alt="Foto" width="90" height="90" style="border-radius: 8px; object-fit: cover; border: 1px solid #E2E8F0; display: block;" />
                    </td>
                    """, imgUrl);
            }
        }

        String locationHtml = StringUtils.isNotBlank(location)
                ? String.format("""
                    <span style="color: #64748B; font-size: 12px; margin-right: 12px; display: inline-block;">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#64748B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align: -1px; margin-right: 3px; display: inline-block;"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/></svg>
                      %s
                    </span>
                    """, location)
                : "";

        String deliveryHtml = listing.isHasDelivery()
                ? """
                    <span style="color: #059669; font-size: 12px; font-weight: 600; display: inline-block;">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#059669" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align: -2px; margin-right: 3px; display: inline-block;"><path d="M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2"/><path d="M15 18H9"/><path d="M19 18h2a1 1 0 0 0 1-1v-5l-3-4h-5v10"/><circle cx="7" cy="18" r="2"/><circle cx="17" cy="18" r="2"/></svg>
                      Entrega disponível
                    </span>
                    """
                : "";

        return String.format("""
            <div style="margin-bottom: 20px; padding: 16px; border: 1px solid #E2E8F0; border-radius: 12px; background-color: #FFFFFF; border-left: 4px solid #10B981;">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                  %s
                  <td style="vertical-align: top;">
                    <!-- BADGES -->
                    <div style="margin-bottom: 6px;">
                      <span style="display: inline-block; background-color: #EEF2FF; color: #4338CA; font-size: 10px; font-weight: 700; padding: 2px 8px; border-radius: 9999px; border: 1px solid #C7D2FE; margin-right: 6px;">
                        <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="#4338CA" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" style="vertical-align: -1px; margin-right: 3px; display: inline-block;"><circle cx="12" cy="12" r="10"/><path d="m4.93 4.93 4.24 4.24"/><path d="m14.83 9.17 4.24-4.24"/><path d="m14.83 14.83 4.24 4.24"/><path d="m9.17 14.83-4.24 4.24"/></svg>
                        Monitor: %s
                      </span>
                      <span style="display: inline-block; background-color: #ECFDF5; color: #047857; font-size: 10px; font-weight: 700; padding: 2px 8px; border-radius: 9999px; border: 1px solid #A7F3D0; margin-right: 6px; text-transform: uppercase;">MATCH %s</span>
                      <span style="display: inline-block; background-color: #F1F5F9; color: #475569; font-size: 10px; font-weight: 600; padding: 2px 8px; border-radius: 9999px;">%s</span>
                    </div>

                    <!-- TITLE -->
                    <a href="%s" target="_blank" style="color: #0F172A; text-decoration: none; font-size: 14px; font-weight: 700; line-height: 1.4; display: block; margin-bottom: 6px;">%s</a>

                    <!-- PRICE -->
                    <div style="margin-bottom: 10px;">
                      <span style="color: #0F172A; font-size: 16px; font-weight: 800; margin-right: 12px;">%s</span>
                    </div>

                    <!-- DETAILS -->
                    <div style="margin-bottom: 12px;">
                      %s%s
                    </div>

                    <!-- ACTION BUTTONS -->
                    <div>
                      <a href="%s" target="_blank" style="display: inline-block; background-color: #0F172A; color: #FFFFFF; text-decoration: none; padding: 6px 14px; border-radius: 6px; font-size: 12px; font-weight: 600; margin-right: 8px;">Ver anúncio &rarr;</a>
                      <a href="%s" target="_blank" style="display: inline-block; background-color: #F1F5F9; color: #334155; text-decoration: none; padding: 6px 12px; border-radius: 6px; font-size: 12px; font-weight: 600; border: 1px solid #CBD5E1;">Ver no App &rarr;</a>
                    </div>
                  </td>
                </tr>
              </table>
            </div>
            """,
                thumbnailHtml,
                monitorName,
                matchScoreFormatted,
                platform,
                listing.getUrl(),
                StringUtils.abbreviate(listing.getTitle(), 120),
                priceFormatted,
                locationHtml,
                deliveryHtml,
                listing.getUrl(),
                monitorDetailUrl);
    }

    private String formatLocation(ScrapedListing listing) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotBlank(listing.getNeighborhood())) parts.add(listing.getNeighborhood());
        if (StringUtils.isNotBlank(listing.getCity())) parts.add(listing.getCity());
        if (StringUtils.isNotBlank(listing.getState())) parts.add(listing.getState());
        return String.join(", ", parts);
    }
}
