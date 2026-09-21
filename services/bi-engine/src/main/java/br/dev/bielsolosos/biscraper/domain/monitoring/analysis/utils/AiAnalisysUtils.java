package br.dev.bielsolosos.biscraper.domain.monitoring.analysis.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.converter.BeanOutputConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.dev.bielsolosos.biscraper.domain.monitoring.model.dto.scrapper.ScrapedListingDTO;

public class AiAnalisysUtils {
    
    
    /**
     * Transforma os itens coletados em um payload compacto para o prompt, omitindo campos desnecessários/vazios.
     */
    public static String formatBatchForPrompt(List<ScrapedListingDTO> batch) {
        ObjectMapper objectMapper = new ObjectMapper();

        List<Map<String, Object>> list = new ArrayList<>(batch.size());
        for (ScrapedListingDTO item : batch) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("vendor_listing_id", item.vendorListingId());
            map.put("vendor", item.vendor() != null ? item.vendor().name() : "OLX");
            if (item.url() != null && !item.url().isBlank()) {
                map.put("url", item.url());
            }
            map.put("title", item.title());
            if (item.price() != null) {
                map.put("price", item.price());
            }
            if (item.hasDelivery()) {
                map.put("has_delivery", true);
            }
            String loc = (item.city() != null ? item.city() : "") + (item.state() != null ? "/" + item.state() : "");
            if (!loc.isBlank()) {
                map.put("location", loc);
            }
            if (item.description() != null && !item.description().isBlank()) {
                String desc = item.description().strip();
                map.put("description", desc.length() > 450 ? desc.substring(0, 450) + "..." : desc);
            }
            list.add(map);
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return list.toString();
        }
    }

    public static <T> String getJsonSchema(Class<T> object){
        return new BeanOutputConverter<>(object).getJsonSchema();
    }

    /**
     * Sanitiza a resposta bruta em formato textual retornada por modelos de IA (LLM).
     * <p>
     * Modelos de linguagem (LLMs como Gemini, ChatGPT, etc.) frequentemente envolvem a resposta
     * estruturada em blocos de código Markdown (por exemplo: {@code ```json ... ```} ou {@code ``` ... ```}),
     * além de eventuais quebras de linha e espaços nas extremidades.
     * </p>
     * <p>
     * Analisadores estritos como o {@link com.fasterxml.jackson.databind.ObjectMapper} do Jackson
     * esperam exclusivamente a sintaxe JSON direta (iniciando com {@code {} ou {@code []}), falhando
     * com {@code JsonParseException} caso esses delimitadores markdown estejam presentes.
     * </p>
     * <p>
     * Este método remove de forma segura os marcadores de bloco markdown e higieniza a string,
     * permitindo que o payload seja diretamente desserializado em DTOs ou entidades.
     * </p>
     *
     * @param rawResponse Resposta textual bruta obtida diretamente da execução do LLM
     * @return String contendo o conteúdo JSON devidamente sanitizado, ou string vazia se nula/em branco
     */
    public static String sanitizeJson(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return "";
        }
        String cleaned = rawResponse.strip();

        // 1. Se contiver delimitadores markdown de código (```json ... ``` ou ``` ... ```), extrai o miolo
        int codeBlockStart = cleaned.indexOf("```json");
        if (codeBlockStart == -1) {
            codeBlockStart = cleaned.indexOf("```JSON");
        }
        if (codeBlockStart == -1) {
            codeBlockStart = cleaned.indexOf("```");
        }

        if (codeBlockStart != -1) {
            int contentStart = cleaned.indexOf('\n', codeBlockStart);
            if (contentStart != -1) {
                int codeBlockEnd = cleaned.indexOf("```", contentStart);
                if (codeBlockEnd != -1) {
                    cleaned = cleaned.substring(contentStart + 1, codeBlockEnd).strip();
                } else {
                    cleaned = cleaned.substring(contentStart + 1).strip();
                }
            }
        }

        // 2. Se houver texto conversacional antes ou depois do JSON (ex: "Tool output... [...]"), isola os delimitadores JSON
        int firstBracket = cleaned.indexOf('[');
        int lastBracket = cleaned.lastIndexOf(']');
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');

        // Dá preferência ao array se '[' aparecer antes de '{' ou se apenas array existir
        if (firstBracket != -1 && lastBracket != -1 && lastBracket > firstBracket) {
            if (firstBrace == -1 || firstBracket < firstBrace) {
                return cleaned.substring(firstBracket, lastBracket + 1).strip();
            }
        }

        // Se for um objeto JSON
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return cleaned.substring(firstBrace, lastBrace + 1).strip();
        }

        return cleaned;
    }
}

