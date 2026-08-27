package br.dev.bielsolosos.biscraper.core.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(OffsetDateTime.class, new JsonDeserializer<>() {
            @Override
            public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                String text = p.getText();
                if (text == null || text.isBlank()) {
                    return null;
                }
                text = text.trim();
                try {
                    return OffsetDateTime.parse(text);
                } catch (DateTimeParseException e) {
                    try {
                        return LocalDateTime.parse(text).atOffset(ZoneOffset.UTC);
                    } catch (DateTimeParseException ex) {
                        try {
                            return Instant.parse(text).atOffset(ZoneOffset.UTC);
                        } catch (DateTimeParseException ex2) {
                            throw new IOException("Falha ao desserializar data: " + text, ex2);
                        }
                    }
                }
            }
        });

        mapper.registerModule(javaTimeModule);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }
}
