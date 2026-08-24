package br.dev.bielsolosos.biscraper.core.config;

import br.dev.bielsolosos.biscraper.domain.users.model.Role;
import br.dev.bielsolosos.biscraper.domain.users.model.RoleEnum;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.repository.RoleRepository;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    @Bean
    public CommandLineRunner initDefaultAdmin(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            Role adminRole = roleRepository.findByName(RoleEnum.ROLE_ADMIN)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(RoleEnum.ROLE_ADMIN).build()));

            Role userRole = roleRepository.findByName(RoleEnum.ROLE_USER)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(RoleEnum.ROLE_USER).build()));

            if (!userRepository.existsByUsername("admin")) {
                User admin = User.builder()
                        .username("admin")
                        .email("admin@bielsolosos.dev.br")
                        .password(passwordEncoder.encode("admin123"))
                        .active(true)
                        .roles(Set.of(adminRole, userRole))
                        .build();

                userRepository.save(admin);
                log.info("Usuário inicial criado: admin / admin123");
            }
        };
    }
}
