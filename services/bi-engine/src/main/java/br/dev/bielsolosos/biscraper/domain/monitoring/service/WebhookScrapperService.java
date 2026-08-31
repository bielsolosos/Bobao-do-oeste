package br.dev.bielsolosos.biscraper.domain.monitoring.service;

import br.dev.bielsolosos.biscraper.core.enums.ExecutionStatus;
import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.core.enums.WebhookStatus;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.AnalisyFactorySelector;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.AnalisysFactory;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.AnalisysResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapedListing;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapingExecution;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.WebhookEvent;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapedListingDTO;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapeResponseDTO;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.webhook.WebhookIncomingPayload;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ProductMonitorRepository;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ScrapedListingRepository;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.ScrapingExecutionRepository;
import br.dev.bielsolosos.biscraper.domain.monitoring.repository.WebhookEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookScrapperService {

    private final WebhookEventRepository webhookEventRepository;
    private final ScrapingExecutionRepository scrapingExecutionRepository;
    private final ScrapedListingRepository scrapedListingRepository;
    private final ProductMonitorRepository productMonitorRepository;
    private final AnalisyFactorySelector analisysSelector;
    private final ObjectMapper objectMapper;

    @Async("webhookProcessorExecutor")
    @Transactional
    public void processScrappingEvent(WebhookIncomingPayload payload) {
        try {
            if (payload.requestId() != null) MDC.put("requestId", payload.requestId());
            if (payload.jobId() != null) MDC.put("jobId", payload.jobId());

            log.info("=================================================");
            log.info(">>> WEBHOOK RECEBIDO DO SCRAPER PYTHON <<<");
            log.info("Status: '{}'", payload.status());

            ScrapeResponseDTO scrapeResponse = payload.response();
            List<ScrapedListingDTO> items = (scrapeResponse != null && scrapeResponse.items() != null)
                    ? scrapeResponse.items()
                    : Collections.emptyList();

            log.info("Total de anúncios raspados recebidos no payload: {}", items.size());
            log.info("=================================================");

            // 1. Atualiza o registro do WebhookEvent no banco
            Optional<WebhookEvent> existingEvent = webhookEventRepository.findByRequestId(payload.requestId());
            if (existingEvent.isPresent()) {
                WebhookEvent event = existingEvent.get();
                event.setStatus("SUCCESS".equalsIgnoreCase(payload.status()) ? WebhookStatus.PROCESSED : WebhookStatus.FAILED);

                if (payload.response() != null) {
                    event.setRawPayload(objectMapper.valueToTree(payload.response()));
                }

                event.setProcessedAt(OffsetDateTime.now());
                webhookEventRepository.save(event);
                log.debug("WebhookEvent atualizado com sucesso no banco");
            }

            // 2. Localiza a ScrapingExecution vinculada ao requestId
            Optional<ScrapingExecution> executionOpt = scrapingExecutionRepository.findByWebhookEventRequestId(payload.requestId());
            if (executionOpt.isEmpty()) {
                log.warn("Nenhuma ScrapingExecution encontrada para o requestId '{}'.", payload.requestId());
                return;
            }

            ScrapingExecution execution = executionOpt.get();
            ProductMonitor monitor = execution.getProductMonitor();

            log.info("Iniciando análise do payload para Monitor '{}' (ID: {}). Itens: {}",
                    monitor.getName(), monitor.getId(), items.size());

            // 3. Invoca a análise do payload passando monitor, execução e anúncios
            analyzePayload(monitor, execution, items);

        } catch (Exception e) {
            log.error("Erro inesperado ao processar evento de scraping: {}", e.getMessage(), e);
        } finally {
            MDC.remove("requestId");
            MDC.remove("jobId");
        }
    }

    /**
     * Realiza a análise do payload e ingestão dos anúncios para o monitor correspondente.
     * Separa anúncios novos daqueles já existentes (para apenas atualizar preço e última visualização).
     */
    public void analyzePayload(ProductMonitor monitor, ScrapingExecution execution, List<ScrapedListingDTO> listings) {
        log.info("Executando analyzePayload para Monitor '{}' com {} anúncios.",
                monitor != null ? monitor.getName() : "N/A",
                listings != null ? listings.size() : 0);

        if (monitor == null || execution == null || listings == null || listings.isEmpty()) {
            log.warn("Nenhum anúncio ou monitor fornecido para ingestão.");
            if (execution != null) {
                execution.setStatus(ExecutionStatus.SUCCESS);
                execution.setTotalFound(0);
                execution.setNewItemsCount(0);
                scrapingExecutionRepository.save(execution);
            }
            return;
        }

        if (!monitor.isActive()) {
            log.info("Monitor '{}' (ID: {}) está desativado. Ignorando ingestão de {} anúncio(s).",
                    monitor.getName(), monitor.getId(), listings.size());
            execution.setStatus(ExecutionStatus.SUCCESS);
            execution.setTotalFound(listings.size());
            execution.setNewItemsCount(0);
            scrapingExecutionRepository.save(execution);
            return;
        }

        List<ScrapedListing> listingsToSave = new ArrayList<>();
        List<ScrapedListingDTO> newListings = new ArrayList<>();

        OffsetDateTime now = OffsetDateTime.now();

        // 1. Separa itens existentes de novos itens
        for (ScrapedListingDTO item : listings) {
            Optional<ScrapedListing> existingOpt = scrapedListingRepository
                    .findByProductMonitorIdAndVendorAndVendorListingId(monitor.getId(), item.vendor(), item.vendorListingId());

            if (existingOpt.isPresent()) {
                ScrapedListing existing = existingOpt.get();
                log.debug("Anúncio existente encontrado (ID: {}). Atualizando preço e lastSeenAt.", existing.getId());

                existing.setCurrentPrice(item.price());
                if (item.originalPrice() != null) {
                    existing.setOriginalPrice(item.originalPrice());
                }
                existing.setLastExecution(execution);
                existing.setLastSeenAt(item.getParsedScrapedAt() != null ? item.getParsedScrapedAt() : now);

                listingsToSave.add(existing);
            } else {
                newListings.add(item);
            }
        }

        // 2. Processa a análise com IA / regras para os itens novos
        if (!newListings.isEmpty()) {
            AnalisysFactory factory = analisysSelector.getFactory(monitor.getAnalysisType());
            List<AnalisysResponse> analyzedItems = factory.analizeScrappedItens(execution, newListings);

            for (AnalisysResponse analyzed : analyzedItems) {
                ScrapedListingDTO dto = analyzed.listing();
                log.info("Criando novo ScrapedListing para anúncio '{}' (link: {})", dto.vendorListingId(), dto.url());

                ScrapedListing newListing = ScrapedListing.builder()
                        .productMonitor(monitor)
                        .lastExecution(execution)
                        .vendor(dto.vendor())
                        .vendorListingId(dto.vendorListingId())
                        .title(dto.title())
                        .url(dto.url())
                        .description(dto.description())
                        .currentPrice(dto.price())
                        .originalPrice(dto.originalPrice())
                        .state(dto.state())
                        .city(dto.city())
                        .neighborhood(dto.neighborhood())
                        .hasDelivery(dto.hasDelivery())
                        .deliveryType(dto.deliveryType())
                        .images(dto.images() != null ? objectMapper.valueToTree(dto.images()) : null)
                        .matchTier(analyzed.matchTier() != null ? analyzed.matchTier() : MatchTier.NONE)
                        .matchScore(analyzed.matchScore() != null ? analyzed.matchScore() : BigDecimal.ZERO)
                        .extractedSpecs(analyzed.params() != null && !analyzed.params().isEmpty()
                                ? objectMapper.valueToTree(analyzed.params())
                                : null)
                        .publishedAt(dto.getParsedPublishedAt())
                        .firstSeenAt(dto.getParsedScrapedAt() != null ? dto.getParsedScrapedAt() : now)
                        .lastSeenAt(dto.getParsedScrapedAt() != null ? dto.getParsedScrapedAt() : now)
                        .build();

                listingsToSave.add(newListing);
            }
        }

        // 3. Persiste todos os anúncios (novos e atualizados)
        scrapedListingRepository.saveAll(listingsToSave);
        log.info("Persistidos com sucesso {} anúncios (sendo {} novos e {} atualizados).",
                listingsToSave.size(), newListings.size(), listingsToSave.size() - newListings.size());

        // 4. Atualiza a execução com as contagens finais e status
        execution.setStatus(ExecutionStatus.SUCCESS);
        execution.setTotalFound(listings.size());
        execution.setNewItemsCount(newListings.size());
        scrapingExecutionRepository.save(execution);

        // 5. Atualiza o timestamp de última raspagem no monitor
        monitor.setLastScrapedAt(now);
        productMonitorRepository.save(monitor);
    }
}
