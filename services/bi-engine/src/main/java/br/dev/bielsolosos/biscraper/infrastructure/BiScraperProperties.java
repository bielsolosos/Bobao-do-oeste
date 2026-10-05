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

    private String appUrl = "https://bi.bielsolosos.dev.br";
    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Scraper scraper = new Scraper();
    private Email email = new Email();
    private Auth auth = new Auth();

    @Getter
    @Setter
    public static class Auth {
        private Otp otp = new Otp();
        private Invites invites = new Invites();

        @Getter
        @Setter
        public static class Otp {
            private boolean enabled = false;
            private int expirationMinutes = 10;
            private int maxAttempts = 5;
            private int cooldownSeconds = 60;
        }

        @Getter
        @Setter
        public static class Invites {
            private boolean enabled = true;
            private int expirationHours = 48;
        }
    }

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
                "http://localhost:4200",
                "http://localhost:8081",
                "https://bi.bielsolosos.dev.br"
        ));
    }

    @Getter
    @Setter
    public static class Scraper {
        private String baseUrl = "http://localhost:8001";
        private String username = "admin";
        private String password = "admin";
        private String webhookUrl = "http://localhost:8080/api/v1/webhooks/scraper";
    }

    @Getter
    @Setter
    public static class Email {
        private boolean enabled = true;
        private String from = "BI Scraper <fatiarapidaautomation@gmail.com>";
        private Digest digest = new Digest();

        @Getter
        @Setter
        public static class Digest {
            private String cron = "0 0 8,12,16,20 * * *";
            private int windowHours = 4;
            private int maxItems = 4;
        }
    }
}
