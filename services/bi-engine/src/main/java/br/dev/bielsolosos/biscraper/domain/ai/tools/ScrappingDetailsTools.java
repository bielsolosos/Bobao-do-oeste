package br.dev.bielsolosos.biscraper.domain.ai.tools;

import br.dev.bielsolosos.biscraper.core.enums.Vendor;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.ScraperHttpClient;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto.ScrapeDetailRequest;
import br.dev.bielsolosos.biscraper.infrastructure.client.scraper.dto.ScrapeDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScrappingDetailsTools {

    private final ScraperHttpClient scraperHttpClient;

    public record ListingDetailsDto(
            String title,
            String description,
            Map<String, Object> properties
    ) {}

    @Tool(description = "Get additional info from listing, such as full description, detailed specifications and attributes.")
    public ListingDetailsDto getAdditionalInfo(
            @ToolParam(description = "Vendor from site that was scraped (OLX, MERCADO_LIVRE)") Vendor vendor,
            @ToolParam(description = "Scraped listing URL") String url) {
        log.info("Tool ScrappingDetailsTools chamada para vendor={} e url={}", vendor, url);

        try {                                                               // Não baixa as imagens e nem força o browser
            ScrapeDetailResponse response = scraperHttpClient.scrapeDetail(new ScrapeDetailRequest(url, vendor, false, 1, false));

            if (response != null && response.success() && response.data() != null) {
                var data = response.data();
                return new ListingDetailsDto(
                        data.title(),
                        data.description(),
                        data.properties() != null ? data.properties() : Collections.emptyMap()
                );
            }
            log.warn("Falha ao obter detalhes do anúncio {}: {}", url, response != null ? response.errorMessage() : "Resposta nula");
            return new ListingDetailsDto(null, null, Collections.emptyMap());
        } catch (Exception e) {
            log.error("Erro ao executar tool getAdditionalInfo para url {}: {}", url, e.getMessage(), e);
            return new ListingDetailsDto(null, null, Collections.emptyMap());
        }
    }
}
