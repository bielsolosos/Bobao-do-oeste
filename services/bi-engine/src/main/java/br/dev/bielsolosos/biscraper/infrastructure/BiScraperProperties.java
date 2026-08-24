package br.dev.bielsolosos.biscraper.infrastructure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "biscraper")
public class BiScraperProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "dGhpcy1pcy1hLXNlY3VyZS1kZWZhdWx0LXNlY3JldC1rZXktZm9yLWp3dC1hdXRoZW50aWNhdGlvbi0yNTYtYml0cw==";
        private long expiration = 3600000L; // 1 hora
        private long refreshExpiration = 86400000L; // 24 horas
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://bi.bielsolosos.dev.br"
        ));
    }
}
