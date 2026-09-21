package br.dev.bielsolosos.biscraper.core.security;

import br.dev.bielsolosos.biscraper.domain.users.service.CustomUserDetailsService;
import br.dev.bielsolosos.biscraper.infrastructure.BiScraperProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private BiScraperProperties properties;

    @Mock
    private BiScraperProperties.Cors corsProperties;

    @Test
    @DisplayName("Deve instanciar BCryptPasswordEncoder e validar senhas")
    void passwordEncoderValidation() {
        SecurityConfig config = new SecurityConfig(userDetailsService, jwtAuthenticationFilter, properties);
        PasswordEncoder encoder = config.passwordEncoder();

        assertNotNull(encoder);
        String encoded = encoder.encode("senha123");
        assertTrue(encoder.matches("senha123", encoded));
        assertFalse(encoder.matches("outrasenha", encoded));
    }

    @Test
    @DisplayName("Deve configurar CORS com origens permitidas corretas")
    void corsConfigurationValidation() {
        when(properties.getCors()).thenReturn(corsProperties);
        when(corsProperties.getAllowedOrigins()).thenReturn(List.of("http://localhost:4200"));

        SecurityConfig config = new SecurityConfig(userDetailsService, jwtAuthenticationFilter, properties);
        CorsConfigurationSource source = config.corsConfigurationSource();

        assertNotNull(source);
        assertTrue(source instanceof UrlBasedCorsConfigurationSource);
    }
}
