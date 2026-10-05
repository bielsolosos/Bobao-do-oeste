package br.dev.bielsolosos.biscraper.domain.users.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.dev.bielsolosos.biscraper.core.exception.BusinessException;
import br.dev.bielsolosos.biscraper.domain.users.model.User;
import br.dev.bielsolosos.biscraper.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reference code:
 * https://github.com/bielsolosos/Noto-Platform/blob/main/services/backend/src/main/java/br/dev/bielsolosos/noto/domain/users/service/UserService.java
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final MeService meService;

    public User changePassword(User user, String oldPassword, String newPassword) {
        log.debug("Tentativa de alteração de senha para o usuário com ID: {}", user.getId());

        verifyUserIntegrity(user);

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("Tentativa de alteração de senha com senha atual incorreta para usuário: {}", user.getUsername());
            throw new BusinessException("Senha atual incorreta");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        User savedUser = repository.save(user);

        log.info("Senha alterada com sucesso para o usuário: {}", user.getUsername());
        return savedUser;
    }

    public User findUserByUsername(String username) {
        return repository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
    }

    public User editUser(User entity, String username, String email) {

        verifyUserIntegrity(entity);

        if (!entity.getUsername().equals(username)) {
            repository.findByUsername(username).ifPresent(u -> {
                throw new BusinessException("Nome de usuário já existente");
            });
        }

        if (!entity.getEmail().equals(email)) {
            repository.findByEmail(email).ifPresent(u -> {
                throw new BusinessException("Email já existente");
            });
        }

        entity.setUsername(username);
        entity.setEmail(email);

        User savedUser = repository.save(entity);
        log.info("Usuário {} atualizado com sucesso: username='{}', email='{}'", savedUser.getId(), username, email);

        return savedUser;
    }

    private void verifyUserIntegrity(User entity) {
        User me = meService.getMe();
        if (!entity.getId().equals(me.getId())) {
            throw new BadCredentialsException("Sem permissão.");
        }
    }

}
