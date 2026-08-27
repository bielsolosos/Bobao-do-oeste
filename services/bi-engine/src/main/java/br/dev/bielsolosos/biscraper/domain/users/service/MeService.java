package br.dev.bielsolosos.biscraper.domain.users.service;

import br.dev.bielsolosos.biscraper.core.exception.BusinessException;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeService {

    private final UserRepository userRepository;

    public User getMe() {
        log.debug("Buscando informações do usuário autenticado no contexto de segurança");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("Tentativa de acesso sem autenticação válida");
            throw new BusinessException("Usuário não autenticado");
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username).orElseThrow(() -> {
            log.error("Usuário autenticado '{}' não encontrado no banco de dados", username);
            return new BusinessException("Usuário não encontrado");
        });
    }
}
