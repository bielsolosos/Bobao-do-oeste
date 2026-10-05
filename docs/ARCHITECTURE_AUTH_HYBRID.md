# Arquitetura e Implementação de Autenticação Híbrida & Gestão de Convites (User Invites)

Este documento descreve detalhadamente o funcionamento, arquitetura de software, modelo de dados e implementação em código do sistema de **Autenticação Híbrida** (Senha tradicional + Passwordless/MFA por código OTP via e-mail) e do subsistema de **Onboarding Fechado por Convites (User Invites)** desenvolvido para a plataforma Bobão do Oeste (`bi-engine` e `project-ui`).

Este guia foi elaborado para servir como **padrão de referência** reutilizável em outros projetos e microsserviços do ecossistema.

---

## 1. Visão Geral da Arquitetura

O sistema adota uma abordagem de **autenticação híbrida desacoplada e configurável**:
1. **Modo Tradicional:** Usuário e Senha validados com BCrypt.
2. **Modo Passwordless (OTP por E-mail):** Código de uso único de 6 dígitos gerado com `SecureRandom`, persistido em banco na forma de **hash criptográfico**, com tempo de expiração, limite de tentativas e controle anti-spam (cooldown).
3. **Desacoplamento por Variáveis de Ambiente:** Os endpoints e beans de OTP só existem no runtime se explicitamente ativados via variáveis de ambiente (`@ConditionalOnExpression`).
4. **Envio de E-mail Assíncrono via Eventos:** O e-mail de autenticação é despachado através de `NotificationEvent` com flag `transactional: true`, garantindo execução pós-commit em thread assíncrona desacoplada e sem bloqueio por preferências de marketing do usuário.
5. **Emissão de Tokens JWT:** Ambos os métodos convergem para o mesmo contrato de resposta (`TokenResponse`), retornando um **Access Token JWT** stateless de curta duração e um **Refresh Token** de longa duração persistido no banco de dados.

```mermaid
flowchart TD
    subgraph Frontend["Frontend (Angular 22)"]
        UI[LoginComponent]
        Tabs{emailOtpAvailable?}
        PW_Tab[Aba Senha]
        OTP_Tab[Aba Código OTP\nOtpLoginComponent]
        AuthServ[AuthService]
    end

    subgraph ConfigDiscovery["Descoberta de Configurações"]
        GET_Config["GET /api/v1/auth/config"]
    end

    subgraph BackendAuth["Backend Security & Controllers"]
        AuthCtrl[AuthController]
        OtpCtrl["EmailOtpController\n(@ConditionalOnExpression)"]
        SecFilter[SecurityFilter JWT]
    end

    subgraph Services["Camada de Serviços"]
        AuthSvc[AuthService]
        OtpSvc["EmailOtpService\n(@ConditionalOnExpression)"]
        RefSvc[RefreshTokenService]
        EventPub[ApplicationEventPublisher]
    end

    subgraph NotificationEngine["Pipeline Assíncrono de E-mail"]
        Event["NotificationEvent (transactional=true)"]
        PubSvc["NotificationPublisherService\n(@TransactionalEventListener AFTER_COMMIT)"]
        Strategy["NotificationEmailStrategy\n(JavaMailSender SMTP)"]
    end

    subgraph Database["Banco de Dados (PostgreSQL)"]
        Users[(users)]
        OTPs[(email_login_otps)]
        Tokens[(refresh_tokens)]
    end

    UI -->|1. Consulta suporte| GET_Config --> AuthCtrl
    AuthCtrl -->|Retorna flag| Tabs
    Tabs -->|Se falso| PW_Tab
    Tabs -->|Se verdadeiro| PW_Tab & OTP_Tab

    PW_Tab -->|POST /auth/login| AuthCtrl --> AuthSvc --> Users
    AuthSvc --> RefSvc --> Tokens
    AuthSvc -->|TokenResponse| AuthServ

    OTP_Tab -->|1. POST /auth/otp/send| OtpCtrl --> OtpSvc
    OtpSvc -->|Salva OTP Hash| OTPs
    OtpSvc -->|Dispara| EventPub --> Event
    Event --> PubSvc --> Strategy -->|Despacha SMTP| Convidado[Caixa de E-mail do Usuário]

    OTP_Tab -->|2. POST /auth/otp/verify| OtpCtrl --> OtpSvc
    OtpSvc -->|Valida Hash & Expiração| OTPs
    OtpSvc --> RefSvc --> Tokens
    OtpSvc -->|TokenResponse| AuthServ
```

---

## 2. Modelo de Dados (Flyway & JPA)

### 2.1. Tabela de Usuários e Tokens
- `users`: Armazena identidade principal (`id` UUID, `username`, `email`, `password` hash BCrypt, `is_active`).
- `user_roles` e `roles`: Associação Many-to-Many contendo papéis (`ROLE_USER`, `ROLE_ADMIN`).
- `refresh_tokens`: Armazena tokens de renovação vinculados ao usuário, com data de expiração e status revogado.

### 2.2. Tabela de Códigos OTP (`email_login_otps`)
Criada na migration [`V9__create_email_login_otps.sql`](../services/bi-engine/src/main/resources/db/migration/V9__create_email_login_otps.sql):

```sql
CREATE TABLE IF NOT EXISTS email_login_otps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_email_login_otps_user_id ON email_login_otps(user_id);
CREATE INDEX IF NOT EXISTS idx_email_login_otps_expires_at ON email_login_otps(expires_at);
CREATE INDEX IF NOT EXISTS idx_email_login_otps_user_used ON email_login_otps(user_id, used);
```

#### Entidade JPA:
[`EmailLoginOtp.java`](../services/bi-engine/src/main/java/br/dev/bielsolosos/biscraper/domain/users/model/EmailLoginOtp.java):
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "email_login_otps")
public class EmailLoginOtp {

    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "code_hash", nullable = false)
    private String codeHash; // Hash BCrypt do código gerado

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder.Default
    @Column(nullable = false)
    private int attempts = 0;

    @Builder.Default
    @Column(nullable = false)
    private boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
```

> [!IMPORTANT]
> **Segurança de Código em Repouso:** O código OTP de 6 dígitos **nunca** é salvo em texto puro no banco de dados. Ele é hasheado com `passwordEncoder.encode(code)`. Caso o banco de dados sofra vazamento, os códigos de acesso ativos permanecem indecifráveis.

---

## 3. Mapeamento de Propriedades e Feature Flags

No [`BiScraperProperties.java`](../services/bi-engine/src/main/java/br/dev/bielsolosos/biscraper/infrastructure/BiScraperProperties.java):

```java
@Data
public static class Auth {
    private Otp otp = new Otp();

    @Data
    public static class Otp {
        private boolean enabled = false;
        private int expirationMinutes = 10;
        private int maxAttempts = 5;
        private int cooldownSeconds = 60;
    }
}
```

No [`application.yml`](../services/bi-engine/src/main/resources/application.yml):
```yaml
biscraper:
  email:
    enabled: ${EMAIL_ENABLED:false}
    from: ${EMAIL_FROM:Bobão do Oeste <no-reply@bielsolosos.dev.br>}
  auth:
    otp:
      enabled: ${AUTH_EMAIL_OTP_ENABLED:false}
      expiration-minutes: ${AUTH_EMAIL_OTP_EXPIRATION_MINUTES:10}
      max-attempts: ${AUTH_EMAIL_OTP_MAX_ATTEMPTS:5}
      cooldown-seconds: ${AUTH_EMAIL_OTP_COOLDOWN_SECONDS:60}
```

---

## 4. Condicionalidade de Endpoints (`@ConditionalOnExpression`)

Para garantir que o subsistema de OTP sequer exista em memória ou na tabela de rotas do Spring quando desativado:

### 4.1. Controller Dedicado: [`EmailOtpController.java`](../services/bi-engine/src/main/java/br/dev/bielsolosos/biscraper/api/controller/auth/EmailOtpController.java)
```java
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/otp")
@RequiredArgsConstructor
@Tag(name = "Auth OTP", description = "Endpoints de autenticação Passwordless via código OTP por e-mail")
@ConditionalOnExpression("${biscraper.auth.otp.enabled:false} and ${biscraper.email.enabled:false}")
public class EmailOtpController {

    private final EmailOtpService emailOtpService;

    @PostMapping("/send")
    public ResponseEntity<SendOtpResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        return ResponseEntity.ok(emailOtpService.sendOtp(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<TokenResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(emailOtpService.verifyOtp(request));
    }
}
```

### 4.2. Serviço Dedicado: [`EmailOtpService.java`](../services/bi-engine/src/main/java/br/dev/bielsolosos/biscraper/domain/users/service/EmailOtpService.java)
```java
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("${biscraper.auth.otp.enabled:false} and ${biscraper.email.enabled:false}")
public class EmailOtpService {
    ...
}
```

### 4.3. Controller Principal Desacoplado: [`AuthController.java`](../services/bi-engine/src/main/java/br/dev/bielsolosos/biscraper/api/controller/auth/AuthController.java)
O `AuthController` **não injeta `EmailOtpService`**, evitando quebra de injeção de dependência quando a feature estiver desligada. Ele injeta apenas `BiScraperProperties`:
```java
@GetMapping("/config")
public ResponseEntity<AuthConfigResponse> getAuthConfig() {
    boolean emailOtpEnabled = properties.getAuth().getOtp().isEnabled() && properties.getEmail().isEnabled();
    return ResponseEntity.ok(new AuthConfigResponse(emailOtpEnabled));
}
```

---

## 5. Pipeline Assíncrono de Notificação e E-mails Transacionais

Para seguir o padrão do [`AGENTS.md`](../AGENTS.md) de nunca realizar operações pesadas (I/O de rede/SMTP) na thread HTTP:

### 5.1. Flag Transacional no Evento
No [`NotificationEvent.java`](../services/bi-engine/src/main/java/br/dev/bielsolosos/biscraper/domain/notification/event/NotificationEvent.java):
```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEvent {
    private User recipient;
    private NotificationTemplate contentTemplate;
    private Map<String, Object> items;
    private boolean transactional; // Bypass de preferências de anúncios
}
```

### 5.2. Bypass de Preferências na Estratégia de E-mail
No [`NotificationEmailStrategy.java`](../services/bi-engine/src/main/java/br/dev/bielsolosos/biscraper/domain/notification/strategy/impl/NotificationEmailStrategy.java):
```java
if (!properties.getEmail().isEnabled()) {
    log.info("Notificações por e-mail estão desativadas globalmente.");
    return;
}

// Eventos transacionais (ex: códigos de login / MFA) ignoram o bloqueio de anúncios
if (!event.isTransactional()) {
    Optional<UserConfig> configOpt = userConfigRepository.findByUserId(recipient.getId());
    if (configOpt.isEmpty() || !configOpt.get().isEmailEnabled()) {
        log.info("Notificações por e-mail desativadas para o usuário '{}'", recipient.getUsername());
        return;
    }
}

// Despacha via JavaMailSender (MimeMessage UTF-8 HTML)
sendViaSmtp(recipient.getEmail(), subject, html, text);
```

### 5.3. Template de E-mail OTP
Em [`EmailOtpNotificationTemplate.java`](../services/bi-engine/src/main/java/br/dev/bielsolosos/biscraper/domain/users/notification/EmailOtpNotificationTemplate.java):
- Implementa `NotificationTemplate`.
- Retorna `NotificationChannel.EMAIL`.
- Gera HTML responsivo com a identidade visual da marca e o código de 6 dígitos em fonte monospace com grande destaque.

### 5.4. Publicação Desacoplada no `EmailOtpService`
```java
EmailOtpNotificationTemplate template = new EmailOtpNotificationTemplate(user, code, expirationMinutes);
NotificationEvent event = NotificationEvent.builder()
        .recipient(user)
        .contentTemplate(template)
        .transactional(true)
        .build();

eventPublisher.publishEvent(event);
```
O listener [`NotificationPublisherService`](../services/bi-engine/src/main/java/br/dev/bielsolosos/biscraper/domain/notification/service/NotificationPublisherService.java) intercepta o evento com `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)` e `@Async("notificationDispatcherExecutor")`, garantindo que:
1. O banco já comitou o OTP com sucesso antes de despachar o e-mail.
2. A resposta HTTP da API retorna instantaneamente para o frontend sem aguardar a latência do servidor SMTP.

---

## 6. Lógica de Negócio do OTP (`EmailOtpService`)

### 6.1. Geração e Envio (`sendOtp`)
1. **Identificação Flexível:** Aceita tanto `username` quanto `email`.
2. **Checagem de Usuário:** Verifica se o usuário existe, se possui e-mail válido e se a conta está ativa.
3. **Controle Anti-Spam (Cooldown):**
   ```java
   Optional<EmailLoginOtp> lastOtp = otpRepository.findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId());
   if (lastOtp.isPresent()) {
       long elapsedSeconds = Duration.between(lastOtp.get().getCreatedAt(), Instant.now()).getSeconds();
       int cooldown = properties.getAuth().getOtp().getCooldownSeconds();
       if (elapsedSeconds < cooldown) {
           long remaining = cooldown - elapsedSeconds;
           throw new BusinessException("Aguarde " + remaining + " segundos antes de solicitar um novo código.");
       }
   }
   ```
4. **Geração Criptográfica:** Gera 6 dígitos via `SecureRandom` (`100000 + secureRandom.nextInt(900000)`).
5. **Persistência do Hash:** Salva entidade com `codeHash = passwordEncoder.encode(code)`.

### 6.2. Verificação e Emissão de Tokens (`verifyOtp`)
1. **Busca do Código Mais Recente:** Recupera o último registro não utilizado (`used = false`).
2. **Validação de Expiração:**
   ```java
   if (otp.getExpiresAt().isBefore(Instant.now())) {
       otp.setUsed(true);
       otpRepository.save(otp);
       throw new BusinessException("O código de acesso expirou. Solicite um novo.");
   }
   ```
3. **Controle de Tentativas (Brute-force protection):**
   - Se atingir o limite (`maxAttempts`, ex: 5), invalida o OTP (`used = true`) e bloqueia.
   - Em caso de código incorreto, incrementa `attempts++` e informa tentativas restantes.
4. **Sucesso:**
   - Marca o código como consumido (`otp.setUsed(true)`).
   - Emite o Access Token JWT (`jwtUtil.generateToken(username)`) e o Refresh Token persistido.

---

## 7. Implementação no Frontend (Angular 22 Standalone)

A arquitetura no frontend foi dividida em componentes modulares e desacoplados:

### 7.1. Camada de Serviço: [`AuthService.ts`](../apps/web/project-ui/src/app/core/services/auth.service.ts)
Gerencia autenticação através de Angular Signals:
- `isAuthenticated = signal<boolean>(this.hasToken())`
- `currentUser = signal<UserResponse | null>(null)`
- Métodos:
  - `getAuthConfig()`: Consulta `/api/v1/auth/config`.
  - `login(credentials)`: Post `/api/v1/auth/login`.
  - `sendOtp({ identifier })`: Post `/api/v1/auth/otp/send`.
  - `verifyOtp({ identifier, code })`: Post `/api/v1/auth/otp/verify`.
  - `handleAuthSuccess(...)`: Salva tokens no `localStorage`, atualiza `isAuthenticated` e dispara `loadMe()`.

### 7.2. Tela de Login: [`LoginComponent`](../apps/web/project-ui/src/app/features/auth/login/login.component.ts) e [`login.component.html`](../apps/web/project-ui/src/app/features/auth/login/login.component.html)
- Consulta `getAuthConfig()` no `ngOnInit`.
- Se `emailOtpEnabled` for `false`: exibe exclusivamente o formulário tradicional por senha.
- Se for `true`: exibe um seletor visual em abas (`Com Senha` vs `Código por E-mail`).
- Sincronização inteligente: Se o usuário preencher o campo de usuário na aba de senha e alternar para a aba OTP, o identificador é copiado automaticamente.

### 7.3. Subcomponente Especializado: [`OtpLoginComponent`](../apps/web/project-ui/src/app/features/auth/login/components/otp-login/otp-login.component.ts)
- Isolado em subpasta `components/otp-login/`.
- **Etapa 1:** Input de usuário/e-mail com validação e loading.
- **Etapa 2:** Input de 6 dígitos numéricos estilizado (`font-mono text-2xl tracking-[0.4em]`).
- **Timer de Cooldown:** Contador regressivo de 60 segundos com botão de reenvio que se reativa após o término.
- Limpeza de temporizadores no `ngOnDestroy`.

---

## 9. Subsistema de Onboarding por Convites (User Invites)

### 9.1. Motivação e Filosofia de Segurança
Em aplicações SaaS B2B e sistemas corporativos fechados, **não deve haver formulário aberto de autocadastro público** (`/register`). O onboarding por convites garante:
1. **Controle Estrito de Acesso:** Apenas administradores autenticados (`ROLE_ADMIN`) podem convidar novos membros para a plataforma.
2. **Definição Prévia de Papéis (RBAC):** O administrador já determina no momento do envio se o novo usuário terá perfil comum (`ROLE_USER`) ou administrativo (`ROLE_ADMIN`).
3. **Verificação Implícita de E-mail:** O futuro usuário só consegue definir seu `username` e senha caso acesse o link enviado diretamente à sua caixa postal, comprovando posse do endereço.
4. **Proteção Anti-Brute-Force & Token Criptográfico:** O link de ativação contém um token UUID v4 aleatório de uso único, com expiração temporal (padrão de 48 horas) e transição atômica de estados.
5. **Auto-Login Transparente:** Ao aceitar o convite e registrar a senha, o backend emite imediatamente o par de tokens JWT (`AccessToken` + `RefreshToken`), redirecionando o usuário autenticado direto ao `/dashboard`.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Administrador
    participant AdminUI as UI Admin (/invites)
    participant Back as UserInviteService
    participant Notif as NotificationEngine
    actor User as Usuário Convidado
    participant PublicUI as UI Ativação (/accept-invite)
    participant DB as PostgreSQL

    Admin->>AdminUI: Preenche e-mail e Role
    AdminUI->>Back: POST /api/v1/admin/invites
    Back->>DB: Salva UserInvite (Status: PENDING, Token UUID v4)
    Back->>Notif: Publica NotificationEvent (transactional=true)
    Notif-->>User: Envia e-mail com CTA e link exclusivo
    Back-->>AdminUI: 201 Created (UserInviteResponse)

    Note over User,PublicUI: O usuário clica no link do e-mail
    User->>PublicUI: Acessa /accept-invite?token={token}
    PublicUI->>Back: GET /api/v1/auth/invites/validate?token={token}
    Back->>DB: Valida integridade, status PENDING e expires_at
    Back-->>PublicUI: 200 OK (e-mail confirmado, status válido)

    User->>PublicUI: Preenche username e define senha
    PublicUI->>Back: POST /api/v1/auth/invites/accept {token, username, password}
    Back->>DB: Cria User, vincula Role, marca UserInvite ACCEPTED
    Back->>Back: Gera Access Token JWT + Refresh Token
    Back-->>PublicUI: 200 OK (TokenResponse)
    PublicUI->>PublicUI: Salva sessão e redireciona para /dashboard
```

---

## 10. Modelo de Dados de Convites (`user_invites`)

### 10.1. Migration Flyway
Criada na migration [`V10__create_user_invites.sql`](../services/bi-engine/src/main/resources/db/migration/V10__create_user_invites.sql):

```sql
CREATE TABLE IF NOT EXISTS user_invites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    invited_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_invites_token ON user_invites(token);
CREATE INDEX IF NOT EXISTS idx_user_invites_email ON user_invites(email);
CREATE INDEX IF NOT EXISTS idx_user_invites_status ON user_invites(status);
```

### 10.2. Enum de Estados de Ciclo de Vida: [`UserInviteStatus.java`](../services/bi-engine/src/main/java/br/dev/bielsolosos/biscraper/core/enums/UserInviteStatus.java)
- `PENDING`: Convite emitido e aguardando ativação pelo convidado.
- `ACCEPTED`: Convite ativado com sucesso; usuário cadastrado no sistema.
- `EXPIRED`: Prazo de validade esgotado (avaliado dinamicamente ou via rotina).
- `CANCELLED`: Revogado manualmente por um administrador antes do aceite.

### 10.3. Entidade JPA: [`UserInvite.java`](../services/bi-engine/src/main/java/br/dev/bielsolosos/biscraper/domain/users/model/UserInvite.java)
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_invites")
public class UserInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 50)
    private String role; // 'ROLE_USER' ou 'ROLE_ADMIN'

    @Column(nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserInviteStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_user_id")
    private User invitedBy;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

---

## 11. Configurações e Notificações de Convite

### 11.1. Propriedades Configuráveis
Definidas em [`BiScraperProperties.java`](../services/bi-engine/src/main/java/br/dev/bielsolosos/biscraper/infrastructure/BiScraperProperties.java) e [`application.yml`](../services/bi-engine/src/main/resources/application.yml):
```yaml
biscraper:
  auth:
    invites:
      enabled: ${AUTH_INVITES_ENABLED:true}
      expiration-hours: ${AUTH_INVITES_EXPIRATION_HOURS:48}
```

### 11.2. Template de E-mail Responsivo: [`UserInviteNotificationTemplate.java`](../services/bi-engine/src/main/java/br/dev/bielsolosos/biscraper/domain/users/notification/UserInviteNotificationTemplate.java)
- Layout HTML responsivo com tipografia moderna e cores institucionais (`#1c1917` e âmbar `#d97706`).
- Botão CTA direcionando para: `{baseUrl}/accept-invite?token={token}`.
- Exibição do prazo de validade em horas e link alternativo textual caso o cliente de e-mail bloqueie botões.

### 11.3. Resiliência de Auditoria para Destinatários Transientes
No pipeline de notificações, a entidade `NotificationLog` exige FK para a tabela `users`. Como o convidado ainda não foi persistido no banco no momento do envio do convite, o método `saveLog` em [`NotificationStrategy.java`](../services/bi-engine/src/main/java/br/dev/bielsolosos/biscraper/domain/notification/strategy/NotificationStrategy.java) foi projetado para:
```java
// Se o destinatário for transiente (ex: convite de novo usuário ainda não cadastrado),
// despacha o e-mail via SMTP normalmente sem falhar na auditoria de logs.
if (event.getRecipient() == null || event.getRecipient().getId() == null) {
    return null;
}
```

---

## 12. Regras de Negócio e Endpoints

### 12.1. Serviço de Domínio: [`UserInviteService.java`](../services/bi-engine/src/main/java/br/dev/bielsolosos/biscraper/domain/users/service/UserInviteService.java)
1. **`createInvite(CreateInviteRequest request)`:**
   - Valida se já existe usuário cadastrado com o e-mail informado.
   - Se já houver convite pendente anterior para esse e-mail, invalida-o marcando como `CANCELLED`.
   - Gera novo token UUID v4 aleatório e calcula `expiresAt = now + expirationHours`.
   - Salva a entidade vinculada ao administrador logado (`meService.getMe()`).
   - Publica o evento assíncrono transacional de e-mail.
2. **`resendInvite(UUID inviteId)`:**
   - Gera novo token criptográfico e renova a data de expiração para mais 48 horas.
   - Atualiza o status para `PENDING` e redispara o e-mail de ativação.
3. **`cancelInvite(UUID inviteId)`:**
   - Marca o status do convite como `CANCELLED`.
4. **`listInvites(UserInviteStatus status, Pageable pageable)`:**
   - Retorna listagem paginada para o painel administrativo.
5. **`validateInvite(String token)`:**
   - Valida se o token existe e está com status `PENDING`.
   - Se `expiresAt.isBefore(now)`, atualiza o status para `EXPIRED` e lança exceção amigável.
   - Retorna dados seguros para o formulário no frontend (e-mail e data de expiração).
6. **`acceptInvite(AcceptInviteRequest request)`:**
   - Executa validação de token e expiração.
   - Valida se o `username` escolhido já está em uso por outro membro.
   - Cria a entidade `User` com senha criptografada via BCrypt.
   - Atribui o papel (`Role`) configurado no momento do convite (`ROLE_USER` ou `ROLE_ADMIN`).
   - Marca o convite como `ACCEPTED` registrando `acceptedAt = Instant.now()`.
   - Invoca `jwtUtil.generateToken(...)` e `refreshTokenService.createRefreshToken(...)` para gerar as credenciais de sessão e retornar um `TokenResponse` completo.

### 12.2. Controladores REST
- **Administrativo (`ROLE_ADMIN`):** [`UserInviteAdminController.java`](../services/bi-engine/src/main/java/br/dev/bielsolosos/biscraper/api/controller/user/UserInviteAdminController.java)
  - `POST /api/v1/admin/invites` (Criação de convite)
  - `GET /api/v1/admin/invites` (Listagem paginada)
  - `POST /api/v1/admin/invites/{id}/resend` (Reenvio)
  - `DELETE /api/v1/admin/invites/{id}` (Cancelamento)
- **Público (Não autenticado):** [`UserInvitePublicController.java`](../services/bi-engine/src/main/java/br/dev/bielsolosos/biscraper/api/controller/auth/UserInvitePublicController.java)
  - `GET /api/v1/auth/invites/validate?token={token}` (Checagem de token)
  - `POST /api/v1/auth/invites/accept` (Submissão de credenciais)

---

## 13. Implementação no Frontend (Angular 22)

### 13.1. Telas e Componentes
1. **Painel de Gestão de Convites ([`InvitesListComponent`](../apps/web/project-ui/src/app/features/admin/invites/invites-list.component.ts)):**
   - Rota `/invites` protegida por `adminGuard`.
   - Exibição de cards estatísticos, formulário dinâmico expansível para envio de convites e tabela paginada.
   - Badges coloridos por status (`PENDING`, `ACCEPTED`, `EXPIRED`, `CANCELLED`).
   - Ações inline com cópia rápida do link direto para a área de transferência (`navigator.clipboard.writeText`), reenvio de e-mail e cancelamento imediato.
2. **Tela Pública de Ativação ([`AcceptInviteComponent`](../apps/web/project-ui/src/app/features/auth/accept-invite/accept-invite.component.ts)):**
   - Rota `/accept-invite?token=...` protegida por `guestGuard`.
   - Captura e valida automaticamente o token na inicialização (`ngOnInit`).
   - Exibe estado visual amigável em caso de link expirado ou inexistente.
   - Formulário com campos de nome de usuário, senha e confirmação de senha, com regras de matching e tamanho mínimo.
   - Ao submeter, utiliza `authService.handleAuthSuccess` para armazenar o JWT e direcionar direto ao dashboard.
3. **Navegação Lateral ([`AppLayoutComponent`](../apps/web/project-ui/src/app/layout/app-layout.component.ts)):**
   - Item "Convites de Usuários" adicionado na categoria **Operação**, condicionado a `isAdmin()`.

---

## 14. Guia para Replicação em Novos Projetos

Para implementar o fluxo completo de **Autenticação Híbrida + Convites** em outro projeto:

1. **Passo a Passo do Backend:**
   - [ ] Aplicar migrations `email_login_otps` (OTP) e `user_invites` (Convites).
   - [ ] Criar entidades `EmailLoginOtp` e `UserInvite`.
   - [ ] Configurar propriedades em `application.yml` (`auth.otp` e `auth.invites`).
   - [ ] Criar templates HTML de notificação e eventos transacionais (`@TransactionalEventListener`).
   - [ ] Configurar rotas públicas no `SecurityConfig` (`/api/v1/auth/invites/**`).
   - [ ] Criar controllers protegidos com `@PreAuthorize("hasRole('ADMIN')")` para emissão de convites.
2. **Passo a Passo do Frontend:**
   - [ ] Implementar serviços `AuthService` e `InviteService`.
   - [ ] Criar rota `/login` com seleção de método (Senha vs OTP por e-mail).
   - [ ] Criar rota protegida `/invites` para administradores gerenciarem acessos.
   - [ ] Criar rota pública `/accept-invite` para ativação de contas e auto-login.
   - [ ] Configurar `adminGuard` e `guestGuard` para proteger as rotas apropriadas.
