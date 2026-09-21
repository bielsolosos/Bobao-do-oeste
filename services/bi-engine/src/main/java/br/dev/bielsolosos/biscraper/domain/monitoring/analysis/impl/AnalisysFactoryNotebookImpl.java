package br.dev.bielsolosos.biscraper.domain.monitoring.analysis.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import br.dev.bielsolosos.biscraper.core.abstractfields.NotebookAnalysisTypeFields;
import br.dev.bielsolosos.biscraper.core.enums.AnalysisType;
import br.dev.bielsolosos.biscraper.core.enums.DiskType;
import br.dev.bielsolosos.biscraper.core.enums.LlmModelEnum;
import br.dev.bielsolosos.biscraper.core.enums.MatchTier;
import br.dev.bielsolosos.biscraper.core.enums.NotebookBrand;
import br.dev.bielsolosos.biscraper.core.enums.ProcessorBrand;
import br.dev.bielsolosos.biscraper.core.enums.ProcessorTier;
import br.dev.bielsolosos.biscraper.core.enums.RamType;
import br.dev.bielsolosos.biscraper.core.enums.ScreenResolution;
import br.dev.bielsolosos.biscraper.domain.ai.model.dto.AiAnalysisLogCreateDto;
import br.dev.bielsolosos.biscraper.domain.ai.service.AiAnalysisLogService;
import br.dev.bielsolosos.biscraper.domain.ai.tools.ScrappingDetailsTools;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.AnalisysFactory;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.AnalisysResponse;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.model.dto.NotebookExtractedSpecsDto;
import br.dev.bielsolosos.biscraper.domain.monitoring.analysis.utils.AiAnalisysUtils;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ProductMonitor;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.ScrapingExecution;
import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapedListingDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AnalisysFactoryNotebookImpl implements AnalisysFactory {

    private static final Pattern INTEL_CORE_GEN_PATTERN = Pattern
            .compile("(?i)(?:i[3579]|core[ -]i[3579])[ -]?(\\d{1,2})\\d{2,3}");
    private static final Pattern GEN_NUM_PATTERN = Pattern
            .compile("(?i)(\\d{1,2})\\s*(?:ª|º|°|o|th)?\\s*(?:ger(?:a[cç][aã]o)?|gen(?:eration)?)");
    private static final Pattern RYZEN_GEN_PATTERN = Pattern
            .compile("(?i)ryzen\\s*[3579]?\\s*(\\d{4})");
    private static final Pattern CORE_ULTRA_PATTERN = Pattern
            .compile("(?i)ultra\\s*[579]?\\s*(\\d)\\d{2}");
    private static final Pattern APPLE_M_PATTERN = Pattern
            .compile("(?i)m(\\d)");

    private static final int BATCH_SIZE = 15;

    private final ChatClient chatClient;
    private final AiAnalysisLogService aiAnalysisLogService;
    private final ScrappingDetailsTools detailsTools;
    private final ObjectMapper nullFallbackObjectmapper = new ObjectMapper()
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            Você é um agente especialista em extração rigorosa e factualmente exata de dados técnicos de Notebooks em marketplaces (OLX, Mercado Livre, etc.).
            Seu objetivo exclusivo é EXTRAIR os dados técnicos reais do equipamento anunciado a partir do resumo ou de ferramentas.

            OBJETIVO E ESCOPO DA BUSCA DO USUÁRIO:
            \"\"\"
            %s
            \"\"\"

            FORMATO DO RESUMO DOS ANÚNCIOS:
            %s

            ========================================================================================
            DIRETRIZES FUNDAMENTAIS ANTI-ALUCINAÇÃO E ANTI-ANCORAGEM (LEI MÁXIMA):
            ========================================================================================
            1. NÃO PROJETE OS TERMOS DE BUSCA DO USUÁRIO NOS ANÚNCIOS:
               - O "OBJETIVO E ESCOPO DA BUSCA DO USUÁRIO" acima descreve APENAS os termos que o usuário digitou na barra de busca do marketplace para filtrar itens.
               - Esses termos NÃO definem o anúncio! Plataformas de marketplace retornam anúncios variados que muitas vezes NÃO têm o processador pesquisado ou omitiram essa especificação no título.
               - NUNCA assuma, deduza ou complete que um anúncio possui determinado processador, geração, RAM ou SSD só porque o termo de busca do usuário continha isso.
               - Se um anúncio disser "Notebook Dell Inspiron 15 8GB 256GB SSD" sem citar o processador (e nenhuma tool foi acionada), os campos 'processorBrand', 'processorModel' e 'processGeneration' DEVEM ser null. NUNCA invente ou adivinhe a CPU!

            2. DESAMBIGUAÇÃO RIGOROSA: CÓDIGO DE CARCAÇA/CHASSIS NÃO É PROCESSADOR:
               - Anúncios frequentemente trazem códigos do modelo da carcaça do notebook. NUNCA confunda código de carcaça com modelo ou geração de processador!
               - EXEMPLOS DE CARCAÇAS (NÃO são processadores nem gerações):
                 * Dell: 'Inspiron 3501', 'Inspiron 3511', 'Inspiron 3520', 'Inspiron 5502', 'Inspiron 5510', 'Inspiron 5402', 'G15 5515', 'G15 5520', 'Latitude 3420', 'Vostro 3500'. (Ex: 'Dell 3501' -> 3501 é a carcaça Dell, NÃO é Core i3 nem geração 35 ou 3!).
                 * Acer: 'Nitro 5', 'Nitro V15', 'Aspire 3 (A315)', 'Aspire 5 (A515)', 'Predator Helios'. (Ex: 'Acer Nitro 5 i7 10750H' -> o processador é 'Core i7-10750H'. 'Nitro 5' é apenas o nome da linha gamer da Acer, NÃO é Ryzen 5 nem i5!).
                 * Lenovo: 'IdeaPad 3', 'IdeaPad 1', 'IdeaPad Gaming 3', 'ThinkPad E14', 'Legion 5'. (Ex: 'Lenovo IdeaPad 3 Ryzen 7 5700U' -> o processador é 'Ryzen 7 5700U'. 'IdeaPad 3' NÃO é Core i3!).
                 * Samsung: 'Book NP550XDA', 'Galaxy Book2 NP750XED', 'Essentials E30'.
                 * Asus: 'VivoBook X515', 'TUF Gaming F15 FX506'.
               - PROCESSADORES REAIS (formatos aceitos para 'processorModel'):
                 * Intel: 'Core i3-1115G4', 'Core i5-1135G7', 'Core i7-10750H', 'Core i7-8550U', 'Core i5-12450H', 'Core i7-13700H', 'Core Ultra 7 155H', 'Celeron N4020', 'Pentium Gold 7505', etc.
                 * AMD: 'Ryzen 3 3200U', 'Ryzen 5 5500U', 'Ryzen 7 5700U', 'Ryzen 5 7530U', 'Ryzen 7 6800H', 'Ryzen 9 7940HS', 'Athlon 3000G', etc.
                 * Apple: 'M1', 'M1 Pro', 'M1 Max', 'M2', 'M2 Pro', 'M3', 'M3 Pro', 'M3 Max', 'M4'.
                 * Qualcomm: 'Snapdragon X Elite', 'Snapdragon X Plus'.

            3. REGRAS DE EXTRAÇÃO DA GERAÇÃO ('processGeneration'):
               - Intel Core: Extraia o número inteiro da geração a partir do código do modelo (ex: 'i5-1135G7' -> 11, 'i7-10750H' -> 10, 'i7-8550U' -> 8, 'i5-7200U' -> 7, 'i5-12450H' -> 12, 'i7-13700H' -> 13) ou se explicitamente escrito (ex: 'i5 11ª geração' -> 11). Se for Core Ultra Série 1 (ex: Ultra 7 155H), use 1.
               - AMD Ryzen: Extraia a família de geração (ex: 'Ryzen 5 5500U' -> 5000 ou 5, 'Ryzen 7 7730U' -> 7000 ou 7, 'Ryzen 5 3500U' -> 3000 ou 3).
               - Apple: 'M1' -> 1, 'M2' -> 2, 'M3' -> 3, 'M4' -> 4.
               - NUNCA extraia geração a partir do número da carcaça do notebook (ex: 'Inspiron 3501' NÃO tem geração 3 nem 35; se o processador for apenas 'Core i5' sem modelo exato, 'processGeneration' DEVE ser null).

            ========================================================================================
            DIRETRIZES DE TRIAGEM E USO DE TOOLS ('getAdditionalInfo' e 'getAdditionalInfoAndImages'):
            ========================================================================================
            1. NÃO CHAME TOOLS PARA PRODUTOS FORA DO ESCOPO: Se o anúncio for claramente de marca incompatível com o que o usuário busca (ex: usuário busca DELL, mas o anúncio é Apple MacBook, Lenovo, Sony, etc.), ou for peças/carcaças/sucatas/acessórios, APENAS extraia os dados básicos presentes no título/resumo. NUNCA acione tools para produtos incompatíveis.
            2. USO CIRÚRGICO DE TOOLS: Acione a tool 'getAdditionalInfo' EXCLUSIVAMENTE para anúncios candidatos que pertençam à marca/linha desejada MAS cujos detalhes vitais (ex: geração/modelo exato do processador, RAM ou capacidade de SSD) estejam ausentes ou incompletos no título/resumo.
            3. DADOS JÁ CLAROS NO RESUMO: Se o título/descrição inicial já contiver os dados necessários (ex: 'Notebook Dell Inspiron i5 1135G7 16GB SSD 512GB'), NÃO acione nenhuma tool.
            4. Se for analisar imagens de candidatos válidos com 'getAdditionalInfoAndImages', atente-se a fotos da tela de 'Sobre o Computador / Propriedades do Sistema' e adesivos no palmrest (adesivos ao lado do touchpad indicando Intel Core i3/i5/i7, AMD Ryzen, geração, GeForce, etc.).

            CAMPOS A EXTRAIR POR ANÚNCIO:
            - brand: Marca do Notebook (APPLE, DELL, LENOVO, ACER, ASUS, HP, SAMSUNG, AVELL, LG, VAIO, MSI, ALIENWARE, OTHER)
            - processorBrand: Fabricante do processador (INTEL, AMD, APPLE, QUALCOMM)
            - processorModel: Nome/modelo do processador (ex: 'Core i5-1135G7', 'Ryzen 5 5500U', 'Core i5', 'M1 Pro', 'Celeron N4020') ou null
            - processGeneration: Número inteiro da geração (ex: 11, 10, 8, 7, 5000, 1) ou null se não identificada
            - ramSize: Quantidade total de memória RAM em Gigabytes (ex: 8, 16, 32) ou null
            - ramType: Tipo da memória RAM (DDR3, DDR4, DDR5, LPDDR4, LPDDR5) ou null
            - storageSizeGb: Tamanho do armazenamento principal em Gigabytes (ex: 128, 256, 512, 1024 para 1TB) ou null
            - diskType: Tipo de disco (SSD, SSD_NVME, SSD_SATA, HDD, EMMC) ou null
            - screenResolution: Resolução da tela (HD, FULL_HD, WUXGA, QHD_2K, WQXGA_2K, UHD_4K, RETINA) ou null
            - hasGpu: Se possui placa de vídeo dedicada (true se tiver GeForce GTX/RTX, Radeon dedicada; false se integrada) ou null

            FORMATO DO ESQUEMA DE SAÍDA:
            %s

            REQUISITO ESTRITO DE RESPOSTA:
            A sua resposta DEVE ser EXCLUSIVAMENTE um array JSON iniciando com '[' e terminando com ']' correspondente à quantidade exata de anúncios fornecidos neste lote.
            NÃO inclua nenhuma conversa, introdução, relatório ou justificativa antes ou depois do JSON.
            Caso não consiga identificar algum campo específico, preencha o campo como null.
            """;

    private record AiExtractedItem(
            NotebookBrand brand,
            ProcessorBrand processorBrand,
            String processorModel,
            Integer processGeneration,
            Integer ramSize,
            RamType ramType,
            Integer storageSizeGb,
            DiskType diskType,
            ScreenResolution screenResolution,
            Boolean hasGpu) {
    }

    public AnalisysFactoryNotebookImpl(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            ObjectMapper objectMapper,
            AiAnalysisLogService aiAnalysisLogService,
            ScrappingDetailsTools detailsTools) {
        this.aiAnalysisLogService = aiAnalysisLogService;
        this.chatClient = initChatClient(chatClientBuilderProvider);
        this.detailsTools = detailsTools;
    }

    @Override
    public AnalysisType getAnalisysType() {
        return AnalysisType.NOTEBOOK;
    }

    @Override
    public List<AnalisysResponse> analizeScrappedItens(ScrapingExecution execution, List<ScrapedListingDTO> listings) {
        if (listings == null || listings.isEmpty()) {
            log.debug("Lista de anúncios vazia ou nula. Nenhuma análise de notebook necessária.");
            return Collections.emptyList();
        }

        ProductMonitor monitor = execution != null ? execution.getProductMonitor() : null;
        log.info("Iniciando análise especializada de NOTEBOOK via Gemini para {} anúncios do monitor '{}'.",
                listings.size(), monitor != null ? monitor.getName() : "N/A");

        if (!isChatClientAvailable(execution, listings.size())) {
            return Collections.emptyList();
        }

        String userCriteria = extractUserCriteria(monitor);
        List<AnalisysResponse> results = new ArrayList<>(listings.size());

        for (int i = 0; i < listings.size(); i += BATCH_SIZE) {
            List<ScrapedListingDTO> batch = listings.subList(i, Math.min(i + BATCH_SIZE, listings.size()));
            results.addAll(analyzeBatch(execution, batch, userCriteria, monitor));
        }

        return results;
    }

    private List<AnalisysResponse> analyzeBatch(
            ScrapingExecution execution,
            List<ScrapedListingDTO> batch,
            String userCriteria,
            ProductMonitor monitor) {

        String itemsJson = AiAnalisysUtils.formatBatchForPrompt(batch);
        String modelName = LlmModelEnum.GEMINI_2_5_FLASH_LITE.getModel();
        long timerStart = System.currentTimeMillis();

        String systemPrompt = String.format(
                SYSTEM_PROMPT_TEMPLATE,
                userCriteria,
                AiAnalisysUtils.getJsonSchema(ScrapedListingDTO.class),
                AiAnalisysUtils.getJsonSchema(AiExtractedItem.class));

        try {
            log.debug("Disparando prompt de extração com Tools para lote de {} anúncios usando modelo '{}'...",
                    batch.size(), modelName);

            ChatResponse response = chatClient.prompt()
                    .tools(detailsTools)
                    .options(ChatOptions.builder()
                            .model(modelName)
                            .temperature(0.2))
                    .messages(new SystemMessage(systemPrompt), new UserMessage(itemsJson))
                    .call()
                    .chatResponse();

            long duration = System.currentTimeMillis() - timerStart;
            log.info("Análise LLM de notebooks concluída com sucesso para lote de {} anúncios em {}ms.",
                    batch.size(), duration);

            String rawResponse = response != null && response.getResult() != null
                    && response.getResult().getOutput() != null
                            ? response.getResult().getOutput().getText()
                            : "";

            // Log de Auditoria
            AiAnalysisLogCreateDto dtoBuilder = AiAnalysisLogCreateDto.fromResponse(response)
                    .productMonitor(monitor)
                    .scrapingExecution(execution)
                    .modelName(modelName)
                    .itemsCount(batch.size())
                    .systemPrompt(systemPrompt)
                    .userPrompt(itemsJson)
                    .rawResponse(rawResponse)
                    .status("SUCCESS")
                    .durationMs((int) duration)
                    .build();

            aiAnalysisLogService.saveLog(dtoBuilder);

            String sanitizedJson = AiAnalisysUtils.sanitizeJson(rawResponse);
            AiExtractedItem[] itensToAnalyze = this.nullFallbackObjectmapper.readValue(
                    sanitizedJson,
                    AiExtractedItem[].class);

            List<AnalisysResponse> itensAnalized = new ArrayList<>();

            for (int i = 0; i < Math.min(itensToAnalyze.length, batch.size()); i++) {
                AiExtractedItem currentAnalisys = itensToAnalyze[i];
                ScrapedListingDTO listing = batch.get(i);
                AnalisysResponse itemAnalized = analyzeExtractedItems(currentAnalisys, listing, execution, monitor);

                if (itemAnalized != null) {
                    itensAnalized.add(itemAnalized);
                }
            }

            return itensAnalized;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - timerStart;
            log.error("Erro na análise do lote de notebooks via Gemini: {}. Gravando log de erro e aplicando fallback.",
                    e.getMessage(), e);

            AiAnalysisLogCreateDto errorDto = AiAnalysisLogCreateDto.builder()
                    .productMonitor(monitor)
                    .scrapingExecution(execution)
                    .modelName(modelName)
                    .itemsCount(batch.size())
                    .systemPrompt(systemPrompt)
                    .userPrompt(itemsJson)
                    .status("ERROR")
                    .durationMs((int) duration)
                    .errorMessage("Não foi possível analisar lote utilizando IA. " + e.getMessage())
                    .build();

            aiAnalysisLogService.saveLog(errorDto);

            if (execution != null) {
                execution.setUsedFallback(true);
            }
            return Collections.emptyList();
        }
    }

    private String extractUserCriteria(ProductMonitor monitor) {
        if (monitor == null) {
            return "Busca geral por notebooks.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Monitor: ").append(monitor.getName() != null ? monitor.getName() : "Notebook");
        if (monitor.getSearchQueries() != null && !monitor.getSearchQueries().isEmpty()) {
            List<String> terms = monitor.getSearchQueries().stream()
                    .map(q -> q.getQueryTerm())
                    .filter(t -> t != null && !t.isBlank())
                    .toList();
            if (!terms.isEmpty()) {
                sb.append(" | Termos de busca: ").append(terms);
            }
        }
        if (monitor.getExpectedSpecs() != null && !monitor.getExpectedSpecs().isEmpty()) {
            try {
                NotebookAnalysisTypeFields fields = new NotebookAnalysisTypeFields(monitor.getExpectedSpecs());
                if (fields.getBrands() != null && !fields.getBrands().isEmpty()) {
                    sb.append(" | Marcas desejadas: ").append(fields.getBrands());
                }
                if (fields.getProcessorVendors() != null && !fields.getProcessorVendors().isEmpty()) {
                    sb.append(" | Processadores: ").append(fields.getProcessorVendors());
                }
                if (fields.getProcessorTiers() != null && !fields.getProcessorTiers().isEmpty()) {
                    sb.append(" | Níveis de CPU: ").append(fields.getProcessorTiers());
                }
                if (fields.getMinimumProcessorGeneration() != null) {
                    sb.append(" | Geração mínima: ").append(fields.getMinimumProcessorGeneration());
                }
                if (fields.getMinimumRamGb() != null) {
                    sb.append(" | RAM mínima: ").append(fields.getMinimumRamGb()).append("GB");
                }
                if (fields.getMinimumStorageGb() != null) {
                    sb.append(" | Armazenamento mínimo: ").append(fields.getMinimumStorageGb()).append("GB");
                }
                if (fields.getNeedsDedicatedGpu() != null) {
                    sb.append(" | Exige GPU dedicada: ").append(fields.getNeedsDedicatedGpu());
                }
            } catch (Exception e) {
                log.debug("Não foi possível detalhar expectedSpecs no prompt: {}", e.getMessage());
            }
        }
        return sb.toString();
    }

    /**
     * Motor determinístico de avaliação e pontuação de anúncios de notebooks.
     * <p>
     * <b>Filosofia de Avaliação:</b>
     * <ul>
     * <li>A IA (Gemini) atua exclusivamente como coletora fiel de dados brutos
     * (extração das especificações).</li>
     * <li>O código Java atua como motor de regras de negócio determinístico,
     * evitando alucinações de score.</li>
     * <li>Cada anúncio inicia com pontuação máxima (100 pontos) e vai perdendo
     * pontos conforme divergências com os critérios do usuário.</li>
     * <li>Campos não preenchidos no monitor (nulos/vazios) são tratados como
     * "Curingas / Aceita Qualquer", não aplicando penalidade.</li>
     * <li>Divergências críticas (ex: marca de processador incompatível ou hardware
     * obsoleto) disparam reprovação imediata ({@code isReproved = true}, score 0 e
     * {@code MatchTier.NONE}).</li>
     * </ul>
     * </p>
     *
     * @param currentAnalisys Dados técnicos brutos extraídos pelo LLM
     * @param listing         Anúncio coletado original (metadados do scraping)
     * @param execution       Execução de scraping associada
     * @param monitor         Configuração de monitoramento do usuário (com os
     *                        critérios estruturados)
     * @return {@link AnalisysResponse} contendo o score final, tier e o payload
     *         JSONB tipado
     */
    private AnalisysResponse analyzeExtractedItems(
            AiExtractedItem currentAnalisys,
            ScrapedListingDTO listing,
            ScrapingExecution execution,
            ProductMonitor monitor) {

        int score = 100;
        boolean isReproved = false;
        List<String> evaluationNotes = new ArrayList<>();

        NotebookAnalysisTypeFields fieldsForAnalise = monitor != null && monitor.getExpectedSpecs() != null
                ? new NotebookAnalysisTypeFields(monitor.getExpectedSpecs())
                : new NotebookAnalysisTypeFields();

        // =========================================================================
        // 1. AVALIAÇÃO DO PROCESSADOR (CPU)
        // =========================================================================
        ProcessorBrand procBrand = currentAnalisys.processorBrand();
        String procModel = currentAnalisys.processorModel();
        Integer procGen = extractGeneration(procBrand, currentAnalisys.processGeneration(), procModel);
        ProcessorTier inferredTier = inferProcessorTier(procBrand, procModel);

        /*
         * 1.1 FABRICANTE DO PROCESSADOR (processorVendors)
         * - Se o usuário definiu fabricantes (ex: [INTEL, AMD]):
         * - Marca não identificada no anúncio: penalidade leve (-10 pts de incerteza).
         * - Marca divergente (ex: pediu Intel e veio Apple Silicon): REPROVAÇÃO
         * IMEDIATA.
         */
        List<ProcessorBrand> allowedVendors = fieldsForAnalise.getProcessorVendors();
        if (allowedVendors != null && !allowedVendors.isEmpty()) {
            if (procBrand == null) {
                score -= 10;
                evaluationNotes.add("Marca do processador não identificada no anúncio (-10 pts)");
            } else if (!allowedVendors.contains(procBrand)) {
                isReproved = true;
                evaluationNotes.add(
                        "Fabricante de processador incompatível: " + procBrand + " (Esperado: " + allowedVendors + ")");
            }
        }

        /*
         * 1.2 NÍVEL DE DESEMPENHO / TIER DO PROCESSADOR (processorTiers)
         * - Classifica o processador em ENTRY (i3/R3), INTERMEDIATE (i5/R5/M1) ou
         * ADVANCED (i7/i9/R7/M Pro).
         * - Se o processador for inferior ao solicitado:
         * - Pediu ADVANCED e veio ENTRY: -35 pts (discrepância severa de desempenho).
         * - Pediu ADVANCED e veio INTERMEDIATE: -15 pts (ressalva moderada).
         * - Pediu INTERMEDIATE e veio ENTRY: -20 pts.
         * - Se o processador for de nível superior ao solicitado (ex: pediu ENTRY e
         * veio i7): 0 perda (bônus).
         */
        List<ProcessorTier> allowedTiers = fieldsForAnalise.getProcessorTiers();
        if (allowedTiers != null && !allowedTiers.isEmpty() && !isReproved) {
            if (inferredTier == null) {
                score -= 10;
                evaluationNotes
                        .add("Nível de desempenho (Tier) do processador não identificado com precisão (-10 pts)");
            } else if (!allowedTiers.contains(inferredTier)) {
                boolean requestedAdvanced = allowedTiers.contains(ProcessorTier.ADVANCED);
                boolean requestedIntermediate = allowedTiers.contains(ProcessorTier.INTERMEDIATE);

                if (inferredTier == ProcessorTier.ENTRY) {
                    if (requestedAdvanced && !requestedIntermediate) {
                        score -= 35;
                        evaluationNotes.add("Processador básico/entrada (" + inferredTier
                                + ") muito abaixo do nível avançado desejado (-35 pts)");
                    } else {
                        score -= 20;
                        evaluationNotes.add("Processador básico/entrada (" + inferredTier
                                + ") abaixo do nível intermediário desejado (-20 pts)");
                    }
                } else if (inferredTier == ProcessorTier.INTERMEDIATE && requestedAdvanced) {
                    score -= 15;
                    evaluationNotes.add("Processador intermediário (" + inferredTier
                            + ") abaixo do nível avançado desejado (-15 pts)");
                }
            }
        }

        /*
         * 1.3 GERAÇÃO MÍNIMA DO PROCESSADOR (minimumProcessorGeneration)
         * - Avalia a geração para arquiteturas x86 (Intel Core e AMD Ryzen série
         * 5000+).
         * - Apple Silicon (M1+) é isento por ser arquitetura recente e eficiente.
         * - Penalidades graduais:
         * - 1 geração abaixo: -10 pts.
         * - 2 gerações abaixo: -20 pts.
         * - 3 gerações abaixo: -40 pts.
         * - >= 4 gerações abaixo: REPROVAÇÃO IMEDIATA (hardware considerado obsoleto).
         */
        Integer minGen = fieldsForAnalise.getMinimumProcessorGeneration();
        if (minGen != null && minGen > 0 && !isReproved) {
            if (procBrand != ProcessorBrand.APPLE) {
                if (procGen == null) {
                    score -= 10;
                    evaluationNotes.add("Geração do processador não identificada no anúncio (-10 pts)");
                } else {
                    int normalizedGen = procGen >= 1000 ? procGen / 1000 : procGen;
                    int normalizedMin = minGen >= 1000 ? minGen / 1000 : minGen;

                    if (normalizedGen < normalizedMin) {
                        int diff = normalizedMin - normalizedGen;
                        if (diff == 1) {
                            score -= 10;
                            evaluationNotes.add("Geração do processador (" + procGen + ") 1 nível abaixo da mínima ("
                                    + minGen + ") (-10 pts)");
                        } else if (diff == 2) {
                            score -= 20;
                            evaluationNotes.add("Geração do processador (" + procGen + ") 2 níveis abaixo da mínima ("
                                    + minGen + ") (-20 pts)");
                        } else {
                            score -= 40;
                            evaluationNotes.add("Geração do processador (" + procGen
                                    + ") muito antiga em relação à mínima (" + minGen + ") (-40 pts)");
                            if (diff >= 4) {
                                isReproved = true;
                                evaluationNotes.add("Reprovado: geração do processador (" + procGen
                                        + ") considerada obsoleta para o critério exigido.");
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 2. AVALIAÇÃO DA MEMÓRIA RAM
        // =========================================================================
        Integer ramSize = currentAnalisys.ramSize();
        RamType ramType = currentAnalisys.ramType();

        /*
         * 2.1 QUANTIDADE MÍNIMA DE MEMÓRIA RAM (minimumRamGb)
         * - Se ramSize >= minRam: Atende plenamente (0 perda).
         * - Se ramSize < minRam:
         * - 4GB quando o usuário pediu >= 16GB: REPROVAÇÃO IMEDIATA (inviável para
         * multitarefa/trabalho).
         * - Muito abaixo (< metade): -25 pts.
         * - 1 degrau abaixo (ex: 8GB quando pediu 16GB, ou 16GB quando pediu 32GB): -20
         * pts.
         * - Quantidade não informada no anúncio: -15 pts de incerteza.
         */
        Integer minRam = fieldsForAnalise.getMinimumRamGb();
        if (minRam != null && minRam > 0 && !isReproved) {
            if (ramSize == null) {
                score -= 15;
                evaluationNotes.add("Quantidade de memória RAM não informada no anúncio (-15 pts)");
            } else if (ramSize < minRam) {
                if (minRam >= 16 && ramSize <= 4) {
                    isReproved = true;
                    evaluationNotes.add("Reprovado: Memória RAM de " + ramSize
                            + "GB insuficiente para o perfil exigido (" + minRam + "GB).");
                } else if (ramSize <= 4 || ramSize < minRam / 2) {
                    score -= 25;
                    evaluationNotes.add("Memória RAM de " + ramSize + "GB muito abaixo do mínimo desejado (" + minRam
                            + "GB) (-25 pts)");
                } else {
                    score -= 20;
                    evaluationNotes.add(
                            "Memória RAM de " + ramSize + "GB abaixo do mínimo desejado (" + minRam + "GB) (-20 pts)");
                }
            }
        }

        /*
         * 2.2 TECNOLOGIAS E GERAÇÕES DE RAM (ramTypes)
         * - Se o usuário exigiu padrões modernos (ex: [DDR5, LPDDR5]):
         * - Anúncio com DDR4: -15 pts (tecnologia anterior, porém ainda utilizável).
         * - Anúncio com DDR3/DDR2/DDR1: -30 pts (padrão obsoleto).
         * - Tipo não especificado no anúncio: -5 pts (penalidade branda, visto que
         * vendedores costumam omitir).
         */
        List<RamType> allowedRamTypes = fieldsForAnalise.getRamTypes();
        if (allowedRamTypes != null && !allowedRamTypes.isEmpty() && !isReproved) {
            if (ramType == null) {
                score -= 5;
                evaluationNotes.add("Tipo de memória RAM não especificado no anúncio (-5 pts)");
            } else if (!allowedRamTypes.contains(ramType)) {
                if (ramType == RamType.DDR3 || ramType == RamType.DDR2 || ramType == RamType.DDR1) {
                    score -= 30;
                    evaluationNotes.add("Tipo de memória " + ramType + " obsoleto em relação aos tipos desejados "
                            + allowedRamTypes + " (-30 pts)");
                } else {
                    score -= 15;
                    evaluationNotes.add("Tipo de memória " + ramType + " divergente dos tipos desejados "
                            + allowedRamTypes + " (-15 pts)");
                }
            }
        }

        // =========================================================================
        // 3. AVALIAÇÃO DE ARMAZENAMENTO & DISCO
        // =========================================================================
        Integer storageSizeGb = currentAnalisys.storageSizeGb();
        DiskType diskType = currentAnalisys.diskType();

        /*
         * 3.1 CAPACIDADE MÍNIMA DE ARMAZENAMENTO (minimumStorageGb)
         * - Se storageSizeGb >= minStorage: Atende 100% ou superior (0 perda).
         * - Se storageSizeGb < minStorage:
         * - <= 64GB quando o usuário exigiu >= 512GB: REPROVAÇÃO IMEDIATA (inviável
         * para a proposta do usuário).
         * - <= 64GB quando o usuário exigiu >= 256GB: -35 pts.
         * - 2 degraus abaixo (< metade, ex: 128GB quando pediu 512GB): -25 pts.
         * - 1 degrau abaixo (ex: 256GB quando pediu 512GB, ou 512GB quando pediu 1TB):
         * -15 pts.
         * - Capacidade não informada no anúncio: -10 pts de incerteza.
         */
        Integer minStorage = fieldsForAnalise.getMinimumStorageGb();
        if (minStorage != null && minStorage > 0 && !isReproved) {
            if (storageSizeGb == null) {
                score -= 10;
                evaluationNotes.add("Capacidade de armazenamento não informada no anúncio (-10 pts)");
            } else if (storageSizeGb < minStorage) {
                if (minStorage >= 512 && storageSizeGb <= 64) {
                    isReproved = true;
                    evaluationNotes.add("Reprovado: Armazenamento de " + storageSizeGb
                            + "GB insuficiente para o perfil exigido (" + minStorage + "GB).");
                } else if (storageSizeGb <= 64) {
                    score -= 35;
                    evaluationNotes.add("Armazenamento de " + storageSizeGb
                            + "GB crítico em relação ao mínimo desejado (" + minStorage + "GB) (-35 pts)");
                } else if (storageSizeGb < minStorage / 2) {
                    score -= 25;
                    evaluationNotes.add("Armazenamento de " + storageSizeGb + "GB muito abaixo do mínimo desejado ("
                            + minStorage + "GB) (-25 pts)");
                } else {
                    score -= 15;
                    evaluationNotes.add("Armazenamento de " + storageSizeGb + "GB abaixo do mínimo desejado ("
                            + minStorage + "GB) (-15 pts)");
                }
            }
        }

        /*
         * 3.2 TECNOLOGIAS DE DISCO (diskTypes)
         * - Compatibilidade inteligente:
         * - Se o usuário pediu SSD genérico, qualquer SSD (NVMe ou SATA) é aceito sem
         * perda.
         * - Se o usuário pediu especificamente SSD_NVME:
         * - SSD_NVME: 0 perda.
         * - SSD genérico: -5 pts (leve incerteza de ser SATA ou NVMe).
         * - SSD_SATA: -10 pts (é SSD, porém taxa de transferência menor).
         * - HD Mecânico (HDD) quando exigido SSD: -30 pts (impacto severo na velocidade
         * do sistema).
         * - Memória Flash básica (EMMC) quando exigido SSD: -35 pts (ou reprovação se
         * exigiu NVMe).
         * - Tipo de disco não informado no anúncio: -10 pts de incerteza.
         */
        List<DiskType> allowedDiskTypes = fieldsForAnalise.getDiskTypes();
        if (allowedDiskTypes != null && !allowedDiskTypes.isEmpty() && !isReproved) {
            if (diskType == null) {
                score -= 10;
                evaluationNotes.add("Tipo de disco não especificado no anúncio (-10 pts)");
            } else if (!isDiskTypeCompatible(diskType, allowedDiskTypes)) {
                if (diskType == DiskType.EMMC) {
                    if (allowedDiskTypes.contains(DiskType.SSD_NVME) && allowedDiskTypes.size() == 1) {
                        isReproved = true;
                        evaluationNotes
                                .add("Reprovado: Armazenamento eMMC básico incompatível com a exigência de SSD NVMe.");
                    } else {
                        score -= 35;
                        evaluationNotes.add("Armazenamento em memória flash eMMC lento em relação aos tipos desejados "
                                + allowedDiskTypes + " (-35 pts)");
                    }
                } else if (diskType == DiskType.HDD) {
                    score -= 30;
                    evaluationNotes.add("Armazenamento em HD mecânico (lento) divergente dos tipos desejados "
                            + allowedDiskTypes + " (-30 pts)");
                } else if (diskType == DiskType.SSD_SATA && allowedDiskTypes.contains(DiskType.SSD_NVME)) {
                    score -= 10;
                    evaluationNotes
                            .add("SSD SATA com taxa de transferência inferior ao padrão NVMe desejado (-10 pts)");
                } else if (diskType == DiskType.SSD && allowedDiskTypes.contains(DiskType.SSD_NVME)) {
                    score -= 5;
                    evaluationNotes.add("SSD não especificado como NVMe (-5 pts)");
                } else {
                    score -= 15;
                    evaluationNotes.add("Tipo de disco " + diskType + " divergente dos tipos desejados "
                            + allowedDiskTypes + " (-15 pts)");
                }
            }
        }

        // =========================================================================
        // 4. AVALIAÇÃO DA PLACA DE VÍDEO (GPU)
        // =========================================================================
        Boolean hasGpu = currentAnalisys.hasGpu();
        Boolean needsDedicatedGpu = fieldsForAnalise.getNeedsDedicatedGpu();

        /*
         * 4.1 EXIGÊNCIA DE GPU DEDICADA (needsDedicatedGpu)
         * - Se needsDedicatedGpu == true (Exige placa dedicada GeForce / Radeon):
         * - Anúncio com GPU dedicada confirmada (hasGpu == true): Atende plenamente (0
         * perda).
         * - Anúncio sem GPU dedicada / apenas integrados (hasGpu == false): REPROVAÇÃO
         * IMEDIATA (inviável para perfil gamer/render).
         * - GPU não confirmada no anúncio (hasGpu == null): -15 pts de incerteza.
         * - Se needsDedicatedGpu == false ou null: Aceita qualquer configuração gráfica
         * (0 perda).
         */
        if (Boolean.TRUE.equals(needsDedicatedGpu) && !isReproved) {
            if (hasGpu == null) {
                score -= 15;
                evaluationNotes.add("Presença de placa de vídeo dedicada não confirmada no anúncio (-15 pts)");
            } else if (!hasGpu) {
                isReproved = true;
                evaluationNotes.add("Reprovado: Notebook possui apenas gráficos integrados; GPU dedicada era exigida.");
            }
        }

        // =========================================================================
        // 5. AVALIAÇÃO DE TELA & RESOLUÇÃO
        // =========================================================================
        ScreenResolution screenResolution = currentAnalisys.screenResolution();

        /*
         * 5.1 RESOLUÇÕES DE TELA DESEJADAS (screenResolutions)
         * - Se o usuário definiu resoluções (ex: [FULL_HD, QHD_2K, RETINA]):
         * - Compatível ou Superior: 0 perda.
         * - Anúncio com resolução HD básica (720p) quando exigido Full HD+: -15 pts.
         * - Resolução não informada no anúncio: -5 pts de incerteza.
         */
        List<ScreenResolution> allowedResolutions = fieldsForAnalise.getScreenResolutions();
        if (allowedResolutions != null && !allowedResolutions.isEmpty() && !isReproved) {
            if (screenResolution == null) {
                score -= 5;
                evaluationNotes.add("Resolução da tela não informada no anúncio (-5 pts)");
            } else if (!isResolutionCompatible(screenResolution, allowedResolutions)) {
                if (screenResolution == ScreenResolution.HD) {
                    score -= 15;
                    evaluationNotes.add("Tela com resolução básica HD (720p) inferior ao padrão desejado "
                            + allowedResolutions + " (-15 pts)");
                } else {
                    score -= 10;
                    evaluationNotes.add("Resolução de tela " + screenResolution + " divergente do padrão desejado "
                            + allowedResolutions + " (-10 pts)");
                }
            }
        }

        // =========================================================================
        // 6. AVALIAÇÃO DA MARCA DO FABRICANTE
        // =========================================================================
        NotebookBrand brand = currentAnalisys.brand();

        /*
         * 6.1 MARCAS DE NOTEBOOK ACEITAS (brands)
         * - Se o usuário selecionou marcas específicas (ex: [DELL, LENOVO, APPLE]):
         * - Marca coincidente: 0 perda.
         * - Marca não identificada no anúncio: -10 pts de incerteza.
         * - Marca divergente (ex: anúncio Acer quando filtrou apenas Dell): REPROVAÇÃO
         * IMEDIATA.
         */
        List<NotebookBrand> allowedBrands = fieldsForAnalise.getBrands();
        if (allowedBrands != null && !allowedBrands.isEmpty() && !isReproved) {
            if (brand == null) {
                score -= 10;
                evaluationNotes.add("Marca do notebook não identificada no anúncio (-10 pts)");
            } else if (!allowedBrands.contains(brand)) {
                isReproved = true;
                evaluationNotes.add("Marca de notebook incompatível: " + brand + " (Esperado: " + allowedBrands + ")");
            }
        }

        // =========================================================================
        // FECHAMENTO DA PONTUAÇÃO E TIERS
        // =========================================================================
        if (isReproved || score <= 0) {
            score = 0;
            isReproved = true;
        }

        MatchTier matchTier = isReproved ? MatchTier.NONE : calculateTier(score);
        BigDecimal finalScore = BigDecimal.valueOf(Math.max(0, score));

        NotebookExtractedSpecsDto specsDto = new NotebookExtractedSpecsDto(
                brand,
                procBrand,
                procModel,
                procGen,
                inferredTier,
                currentAnalisys.ramSize(),
                currentAnalisys.ramType(),
                currentAnalisys.storageSizeGb(),
                currentAnalisys.diskType(),
                screenResolution,
                hasGpu,
                isReproved,
                evaluationNotes);

        return new AnalisysResponse(execution, listing, matchTier, finalScore, specsDto.toMap());
    }

    private boolean isResolutionCompatible(ScreenResolution actual, List<ScreenResolution> allowed) {
        if (allowed.contains(actual)) {
            return true;
        }
        int actualRank = getResolutionRank(actual);
        int minAllowedRank = allowed.stream()
                .mapToInt(this::getResolutionRank)
                .min()
                .orElse(0);

        return actualRank >= minAllowedRank;
    }

    private int getResolutionRank(ScreenResolution res) {
        if (res == null)
            return 0;
        return switch (res) {
            case HD -> 1;
            case FULL_HD -> 2;
            case WUXGA -> 3;
            case QHD_2K -> 4;
            case WQXGA_2K, RETINA -> 5;
            case UHD_4K -> 6;
        };
    }

    private boolean isDiskTypeCompatible(DiskType actual, List<DiskType> allowed) {
        if (allowed.contains(actual)) {
            return true;
        }
        // Se o usuário pediu SSD genérico, SSD_NVME e SSD_SATA são ambos tecnologias
        // válidas de SSD
        if (allowed.contains(DiskType.SSD) && (actual == DiskType.SSD_NVME || actual == DiskType.SSD_SATA)) {
            return true;
        }
        return false;
    }

    private ProcessorTier inferProcessorTier(ProcessorBrand brand, String model) {
        if (model == null || model.isBlank())
            return null;
        String m = model.toLowerCase();

        // Código feio mas funcional aqui:
        if (m.contains("i9") || m.contains("ryzen 9") || m.contains("r9") ||
                m.contains("i7") || m.contains("ryzen 7") || m.contains("r7") ||
                m.contains("ultra 7") || m.contains("ultra 9") ||
                m.contains("m1 pro") || m.contains("m1 max") || m.contains("m1 ultra") ||
                m.contains("m2 pro") || m.contains("m2 max") || m.contains("m2 ultra") ||
                m.contains("m3 pro") || m.contains("m3 max") || m.contains("m3 ultra") ||
                m.contains("m4 pro") || m.contains("m4 max") || m.contains("m4 ultra") ||
                m.contains("threadripper") || m.contains("xeon")) {
            return ProcessorTier.ADVANCED;
        }

        if (m.contains("i5") || m.contains("ryzen 5") || m.contains("r5") ||
                m.contains("ultra 5") ||
                m.contains("m1") || m.contains("m2") || m.contains("m3") || m.contains("m4") ||
                m.contains("snapdragon x")) {
            return ProcessorTier.INTERMEDIATE;
        }

        if (m.contains("i3") || m.contains("ryzen 3") || m.contains("r3") ||
                m.contains("celeron") || m.contains("pentium") || m.contains("n100") ||
                m.contains("n200") || m.contains("n4000") || m.contains("n4020") ||
                m.contains("n4500") || m.contains("n5100") || m.contains("athlon") ||
                m.contains("atom")) {
            return ProcessorTier.ENTRY;
        }

        return null;
    }

    private Integer extractGeneration(ProcessorBrand brand, Integer explicitGen, String model) {
        // 1. Tenta extrair deterministicamente do modelo do processador primeiro via regex
        if (model != null && !model.isBlank()) {
            // Intel Core (ex: i5-1135G7 -> 11, i7 8550U -> 8, i3 1005G1 -> 10, i7 13700H -> 13)
            Matcher intelMatcher = INTEL_CORE_GEN_PATTERN.matcher(model);
            if (intelMatcher.find()) {
                try {
                    int gen = Integer.parseInt(intelMatcher.group(1));
                    if (gen >= 1 && gen <= 15) {
                        return gen;
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            // Core Ultra (ex: Ultra 7 155H -> 1, Ultra 7 258V -> 2)
            Matcher ultraMatcher = CORE_ULTRA_PATTERN.matcher(model);
            if (ultraMatcher.find()) {
                try {
                    return Integer.parseInt(ultraMatcher.group(1));
                } catch (NumberFormatException ignored) {
                }
            }

            // Padrão explícito de texto (ex: 11ª geração, 7 ger, 10th gen, 12º gen)
            Matcher genMatcher = GEN_NUM_PATTERN.matcher(model);
            if (genMatcher.find()) {
                try {
                    int gen = Integer.parseInt(genMatcher.group(1));
                    if (gen >= 1 && gen <= 15) {
                        return gen;
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            // AMD Ryzen (ex: Ryzen 5 5500U -> 5, Ryzen 7 7730U -> 7, Ryzen 3500U -> 3)
            Matcher ryzenMatcher = RYZEN_GEN_PATTERN.matcher(model);
            if (ryzenMatcher.find()) {
                try {
                    int fullNumber = Integer.parseInt(ryzenMatcher.group(1));
                    return fullNumber / 1000;
                } catch (NumberFormatException ignored) {
                }
            }

            // Apple Silicon (ex: M1 -> 1, M2 -> 2, M3 -> 3, M4 -> 4)
            Matcher appleMatcher = APPLE_M_PATTERN.matcher(model);
            if (appleMatcher.find()) {
                try {
                    return Integer.parseInt(appleMatcher.group(1));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // 2. Se não extraído por regex, valida o explicitGen fornecido
        if (explicitGen != null && explicitGen > 0) {
            // Se for geração inteira direta (1 a 15)
            if (explicitGen <= 15) {
                return explicitGen;
            }
            // Se for família AMD Ryzen em milhares (ex: 5000, 7000)
            if (brand == ProcessorBrand.AMD && explicitGen >= 1000 && explicitGen <= 9000) {
                return explicitGen / 1000;
            }
            // Códigos de chassi como 3501, 5510, 35 são descartados como ruído
        }

        return null;
    }

    private MatchTier calculateTier(int score) {
        if (score >= 85)
            return MatchTier.HIGH;
        if (score >= 65)
            return MatchTier.MEDIUM;
        if (score >= 45)
            return MatchTier.LOW;
        return MatchTier.NONE;
    }

    private boolean isChatClientAvailable(ScrapingExecution execution, int listingsCount) {
        if (this.chatClient == null) {
            log.warn(
                    "ChatClient (Spring AI / Gemini) não disponível no contexto. Aplicando fallback (MatchTier.NONE) para {} anúncios.",
                    listingsCount);
            if (execution != null) {
                execution.setUsedFallback(true);
            }
            return false;
        }
        return true;
    }

    private ChatClient initChatClient(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        return builder != null ? builder.build() : null;
    }
}
