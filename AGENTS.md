# Diretrizes Arquiteturais e Padrões de Projeto (AGENTS.md)

Este documento estabelece a convenção arquitetural, padrões de código e diretrizes de desenvolvimento seguidos no ecossistema de microsserviços e backend Java (Spring Boot 3+ e Java 21+), consolidando as melhores práticas dos projetos de referência (`Noto-Back-end` e `chat-ai`).

---

## 1. Estrutura de Pacotes & Camadas

A aplicação segue uma arquitetura em camadas limpa e desacoplada:

```
br.dev.bielsolosos.<projeto>
├── api
│   ├── controller       # Controllers REST públicos e protegidos (@RestController)
│   ├── mapper           # Mappers dedicados de API (quando não encapsulados no domínio)
│   └── model            # Records para DTOs globais ou de contratos de API
├── core
│   ├── abstractfields   # Contratos polimórficos / schemas extensíveis
│   ├── config           # Configurações gerais de Beans, CORS, OpenAPI, etc.
│   ├── enums            # Todos os Enums da aplicação centralizados
│   ├── exception        # Hierarquia de exceções de negócio e GlobalExceptionHandler
│   ├── security         # Filtros JWT, SecurityConfig, UserDetails
│   └── utils            # Utilitários globais de segurança, criptografia, etc.
├── domain
│   └── <bounded-context>
│       ├── mapper       # Mappers dedicados de conversão DTO <-> Entity do domínio
│       ├── model        # Entidades JPA (@Entity) e subpacote dto (Records)
│       ├── repository   # Interfaces Spring Data JPA / Specifications / Projections
│       └── service      # Regras de negócio, transações, validações e IA/Tools
└── infrastructure       # Properties (@ConfigurationProperties), clientes HTTP e SDKs
```

---

## 2. Padrões de Implementação das Services

As classes de serviço no pacote `domain.*.service` devem seguir a seguinte estrutura:

1. **Injeção de Dependências & Logs:**
   - Usar `@Service`, `@RequiredArgsConstructor` (Lombok) para injeção via construtor. Nunca usar `@Autowired` em atributos.
   - Usar `@Slf4j` para logs contextualizados em níveis apropriados (`debug`, `info`, `warn`, `error`).
2. **Gerenciamento Transacional:**
   - Métodos de leitura utilizam `@Transactional(readOnly = true)`.
   - Métodos que alteram estado utilizam `@Transactional`.
3. **Resolução de Usuário Autenticado:**
   - Obter a entidade do usuário atual através do `MeService.getMe()`.
   - Evitar passar IDs de usuário diretamente do frontend quando a rota for autenticada.
4. **Validação de Permissão & Ownership:**
   - Isolar a checagem de permissão em método privado da service:
     ```java
     private void validatePermission(ProductMonitor entity) {
         User me = meService.getMe();
         if (!entity.getUser().getId().equals(me.getId())) {
             log.warn("Acesso negado: Usuário '{}' tentou acessar recurso '{}' pertencente a outro usuário", me.getUsername(), entity.getId());
             throw new BusinessException("Você não tem permissão para acessar ou modificar este recurso.");
         }
     }
     ```
5. **Tratamento de Exceções de Negócio:**
   - Lançar `BusinessException("Mensagem clara")` capturada pelo `GlobalExceptionHandler`.
6. **Eventos Assíncronos & Background Jobs:**
   - Tarefas pesadas (ex: scraping Playwright, processamento em lote, notificações) devem ser disparadas de forma assíncrona desacoplada (via Spring Events, RabbitMQ ou Jobs).
   - Manter comentários padronizados `// TODO: [Descrição do evento]` durante o scaffold.

---

## 3. DTOs, Mappers & Validação

- **Records Imutáveis:** Todos os DTOs de Request e Response devem ser `record`.
- **Bean Validation:** Sempre incluir anotações do Jakarta Validation (`@NotBlank`, `@NotNull`, `@NotEmpty`, `@Valid`, `@PositiveOrZero`, `@Size`) em DTOs de entrada.
- **Polimorfismo com Jackson:**
  - Utilizar `@JsonTypeInfo` e `@JsonSubTypes` com `include = JsonTypeInfo.As.EXTERNAL_PROPERTY` para campos extensíveis (ex: `AnalysisTypeFields` mapeado para `SimpleAnalisisTypeFields` ou `NotebookAnalysisTypeFields`).
- **Mappers Dedicados:**
  - Localizados em `api.mapper.<contexto>.*Mapper`.
  - Injetar `ObjectMapper` quando houver conversão de objetos para `JsonNode` (colunas PostgreSQL `JSONB`).
  - Fornecer métodos `toEntity`, `toResponse` e `toSummary`.

---

## 4. Controllers REST

- Usar `@Tag` e `@Operation` (OpenAPI/Swagger) para documentação da API.
- Endpoints de listagem paginada devem utilizar `Pageable` com `@PageableDefault` (ex: `size = 10, sort = "createdAt", direction = Sort.Direction.DESC`).
- Retornos utilizando `ResponseEntity<T>` com códigos HTTP semânticos (`201 CREATED`, `200 OK`, `204 NO_CONTENT`).
- Rotas REST padronizadas no plural e kebab-case (ex: `/api/v1/product-monitors`).

---

## 5. Enums Centralizados

- Todos os Enums pertencem ao pacote `br.dev.bielsolosos.<projeto>.core.enums`.
- Enums ricos devem conter propriedades utilitárias quando necessário (ex: `ScrapingFrequency` com a representação em cron expression e descrição legível).
