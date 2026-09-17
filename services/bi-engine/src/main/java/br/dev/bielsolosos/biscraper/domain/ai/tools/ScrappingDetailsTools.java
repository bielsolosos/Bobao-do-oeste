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
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScrappingDetailsTools {

    private final ScraperHttpClient scraperHttpClient;

    public record ListingDetailsDto(
            String title,
            String description,
            List<String> urlImages,
            Map<String, Object> properties
    ) {}
    
    @Tool(description = """
            Busca detalhes completos, descrição integral e atributos técnicos diretamente da página de um anúncio no marketplace.
            REGRAS CRÍTICAS DE USO (NÃO FAÇA SPAM):
            1. SÓ acione esta ferramenta se o anúncio for um CANDIDATO FORTE que pertence à mesma categoria, marca e linha que o usuário pediu.
            2. NUNCA acione esta ferramenta para produtos obviamente incompatíveis (ex: usuário quer PS5 e o anúncio é PS4; quer Placa de Vídeo e é Processador; quer Mac M1 e é notebook Dell/Intel/HP). Nesses casos, descarte o item na sua análise IMEDIATAMENTE SEM chamar a ferramenta.
            3. NUNCA acione esta ferramenta se o título e o resumo fornecidos já tiverem informações suficientes para confirmar com 100%% de certeza que o item atende a todos os requisitos.
            4. USO CIRÚRGICO: Acione a ferramenta APENAS quando o item for um candidato válido com potencial real, MAS o texto resumido for ambíguo, vago ou omitir detalhes técnicos decisivos (ex: omite quantidade de VRAM/RAM, capacidade do SSD, variante exata do chip, versão do console ou estado de conservação).
            """)
    public ListingDetailsDto getAdditionalInfo(
            @ToolParam(description = "Vendor from site that was scraped (OLX, MERCADO_LIVRE)") Vendor vendor,
            @ToolParam(description = "Scraped listing URL") String url) {

        log.info( "======================================================== TOOL ACIONADA =============================================================");
        log.info("Tool ScrappingDetailsTools chamada para vendor={} e url={}", vendor, url);

        try {                                                               // Não baixa as imagens e nem força o browser
            ScrapeDetailResponse response = scraperHttpClient.scrapeDetail(new ScrapeDetailRequest(url, vendor, false, 24, false));

            if (response != null && response.success() && response.data() != null) {
                var data = response.data();
                
                return new ListingDetailsDto(
                        data.title(),
                        sanitizeDescription(data.description()),
                        null,
                        data.properties() != null ? data.properties() : Collections.emptyMap()
                );
            }
            log.warn("Falha ao obter detalhes do anúncio {}: {}", url, response != null ? response.errorMessage() : "Resposta nula");
            return new ListingDetailsDto(null, null, null, Collections.emptyMap());
        } catch (Exception e) {
            log.error("Erro ao executar tool getAdditionalInfo para url {}: {}", url, e.getMessage(), e);
            return new ListingDetailsDto(null, null, null, Collections.emptyMap());
        }
    }

    @Tool(description = """
            Busca detalhes completos, descrição integral, atributos técnicos e fotos diretamente da página de um anúncio no marketplace.
            UTILIZE ESTA FERRAMENTA no lugar de getAdditionalInfo APENAS se fotos forem estritamente necessárias para verificar o estado físico/conservação do produto.
            REGRAS CRÍTICAS DE USO (NÃO FAÇA SPAM):
            1. SÓ acione esta ferramenta se o anúncio for um CANDIDATO FORTE que pertence à mesma categoria, marca e linha que o usuário pediu.
            2. NUNCA acione esta ferramenta para produtos obviamente incompatíveis. Nesses casos, descarte o item imediatamente sem chamar a ferramenta.
            3. NUNCA acione esta ferramenta se o título e o resumo fornecidos já tiverem informações suficientes para confirmar o item.
            4. USO CIRÚRGICO: Acione apenas para candidatos com potencial real cujos detalhes técnicos ou estado visual sejam ambíguos ou incompletos.
            """)
    public ListingDetailsDto getAdditionalInfoAndImages(
            @ToolParam(description = "Vendor from site that was scraped (OLX, MERCADO_LIVRE)") Vendor vendor,
            @ToolParam(description = "Scraped listing URL") String url) {

        log.info("======================================================== TOOL IMAGEM ACIONADA =============================================================");
        log.info("Tool ScrappingDetailsTools chamada para vendor={} e url={}", vendor, url);

        try {
            ScrapeDetailResponse response = scraperHttpClient.scrapeDetail(new ScrapeDetailRequest(url, vendor, true, 24, false));

            if (response != null && response.success() && response.data() != null) {
                var data = response.data();

                return new ListingDetailsDto(
                        data.title(),
                        sanitizeDescription(data.description()),
                        data.cachedImages() != null
                                ? data.cachedImages().stream().map(item -> item.fullEndpointUrl()).toList()
                                : Collections.emptyList(),
                        data.properties() != null ? data.properties() : Collections.emptyMap()
                );
            }
            log.warn("Falha ao obter detalhes do anúncio {}: {}", url, response != null ? response.errorMessage() : "Resposta nula");
            return new ListingDetailsDto(null, null, null, Collections.emptyMap());
        } catch (Exception e) {
            log.error("Erro ao executar tool getAdditionalInfoAndImages para url {}: {}", url, e.getMessage(), e);
            return new ListingDetailsDto(null, null, null, Collections.emptyMap());
        }
    }

    private String sanitizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String cleaned = description.replaceAll("\\R{2,}", "\n").strip();
        return cleaned.length() > 3500 ? cleaned.substring(0, 3500) + "..." : cleaned;
    }
}
