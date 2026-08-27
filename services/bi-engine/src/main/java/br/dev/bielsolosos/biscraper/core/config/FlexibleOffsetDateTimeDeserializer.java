package br.dev.bielsolosos.biscraper.core.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

public class FlexibleOffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {

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
}
