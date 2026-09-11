# Plano de Identidade Visual e Redesign da SPA

Este documento transforma a auditoria visual da SPA em um plano de implementação. O objetivo não é apenas trocar cores e imagens, mas alinhar marca, arquitetura de informação, acessibilidade e experiência ao valor central do produto: encontrar oportunidades relevantes em marketplaces.

## Estado deste trabalho

- [x] Inventário dos assets visuais existentes.
- [x] Aplicação dos assets no README raiz.
- [x] Uso do emblema SVG como favicon da SPA.
- [x] Correção inicial de idioma, título, descrição e cor de tema do documento HTML.
- [x] Definição final do nome público da aplicação (**Bobão do Oeste**).
- [x] Criação dos tokens e componentes do design system (tokens em `styles.css` e primitives compartilhados).
- [x] Redesign das telas e dos fluxos (login, dashboard, oportunidades, monitores, eventos e auditoria).
- [x] Testes unitários dos componentes compartilhados e execução na CI.
- [ ] Validação de acessibilidade automatizada (axe) e regressão visual.
- [ ] Paginação e filtros no servidor para as listagens globais.

### Decisões confirmadas

1. Nome público: **Bobão do Oeste**, com o descritor "Monitor inteligente de oportunidades".
2. A imagem do caçador armado fica restrita ao README; no produto usa-se a cena do robô analisando a GPU.
3. Eventos e Auditoria de IA são visíveis apenas para `ROLE_ADMIN`.
4. Dark mode fica para uma fase posterior.

### Limitações conhecidas desta entrega

- Os endpoints de listagem global não aceitam filtros no backend; os filtros de Oportunidades, Monitores, Eventos e Auditoria são aplicados no cliente sobre um lote carregado (até 200 registros).
- A conversão dos JPGs para AVIF/WebP e os recortes responsivos ainda não foram feitos; o login usa um JPEG redimensionado em tempo de build local.
- Não há testes E2E, de axe ou de regressão visual nesta etapa.

## 1. Diagnóstico atual

### 1.1 Posicionamento percebido

A interface atual comunica um painel administrativo técnico. Dashboard, webhooks, filas e auditorias recebem grande destaque, enquanto os anúncios e oportunidades, que representam o valor principal para o usuário, aparecem somente dentro do detalhe de cada monitor.

O produto deve migrar de "console de scraping" para "central de oportunidades", mantendo os detalhes operacionais em uma área secundária para administradores e troubleshooting.

### 1.2 Marca fragmentada

Três nomes aparecem na aplicação:

- `Bobão do Oeste` na sidebar desktop.
- `BI Engine` no login e no cabeçalho móvel.
- `ProjectUi` no título original e no README gerado pelo Angular.

Recomendação: adotar **Bobão do Oeste** como marca do produto e usar **Monitor inteligente de oportunidades** como descritor. `BI Engine` deve permanecer como nome técnico do backend. Essa decisão precisa ser confirmada antes de alterar todos os textos da interface.

Arquivos envolvidos:

- `apps/web/project-ui/src/app/layout/app-layout.component.ts`
- `apps/web/project-ui/src/app/features/auth/login/login.component.ts`
- `apps/web/project-ui/src/index.html`
- `apps/web/project-ui/README.md`

### 1.3 Linguagem visual

A paleta atual usa azul, slate e cinza de forma funcional, mas genérica. Cards brancos, sidebar escura e tabelas densas criam uma boa base administrativa, porém não expressam a personalidade presente nos assets.

O emblema propõe uma direção mais distintiva:

- Carvão e grafite para estrutura e confiança.
- Couro e latão para personalidade.
- Âmbar para energia, radar e descoberta.
- Ciano controlado para dados, links e telemetria.
- Superfícies claras levemente quentes para evitar uma estética excessivamente temática.

A linguagem de faroeste deve aparecer em marca, ilustrações e pequenos detalhes. Formulários, tabelas e gráficos devem continuar sóbrios e previsíveis.

### 1.4 Design system inexistente

Tailwind CSS 4 está instalado, mas `src/styles.css` contém apenas o import do framework. Não existem tokens semânticos de cor, tipografia, espaçamento, radius, sombra, foco ou movimento.

Existem componentes compartilhados úteis, porém eles não são usados de forma consistente:

- `ui-button`
- `ui-card`
- `ui-badge`
- `ui-tabs`
- `ui-pagination`
- `ui-toast`
- `ui-confirm`

Botões, badges, cabeçalhos, modais, estados vazios e spinners continuam duplicados nas features. Isso produz diferenças de padding, radius, foco e comportamento.

### 1.5 Hierarquia e densidade

Os títulos seguem uma escala consistente, mas há uso excessivo de textos com 10 e 11 px em badges, navegação móvel, IDs e payloads. A interface também usa tabelas para quase todos os conjuntos de dados, o que favorece desktop e operação técnica, mas prejudica leitura rápida e uso móvel.

### 1.6 Estados assíncronos

Loading, vazio e erro frequentemente compartilham a mesma apresentação. Em algumas telas, uma falha pode parecer:

- Dashboard com todos os valores iguais a zero.
- Scraper saudável e ocioso.
- Lista legitimamente vazia.
- Carregamento que nunca termina.

Cada consulta precisa representar explicitamente `idle`, `loading`, `success`, `empty` e `error`, com retry contextual.

### 1.7 Responsividade

O shell possui sidebar desktop e bottom navigation móvel, mas as páginas internas continuam orientadas a desktop:

- Cabeçalhos sem quebra adequada.
- Tabelas com rolagem horizontal como única adaptação.
- Muitas ações por linha.
- Formulários com duas colunas em telas estreitas.
- Bottom navigation sem tratamento de safe area.

O objetivo deve ser oferecer cards ou listas resumidas no móvel e tabelas progressivamente aprimoradas a partir de `md` ou `lg`.

### 1.8 Acessibilidade

Os maiores riscos encontrados são:

- Labels sem associação explícita aos inputs.
- Validação sem mensagens por campo e sem descrição acessível.
- Menu de usuário dependente de hover.
- Dialogs sem focus trap, Escape e restauração de foco.
- Botões apenas com ícone sem nome acessível.
- Imagens de anúncios sem texto alternativo.
- Foco removido sem substituição em alguns controles.
- Toast sem live region.
- Tabs sem semântica e navegação de teclado.
- Contraste insuficiente em textos e bordas muito claros.
- Animações sem suporte a `prefers-reduced-motion`.

### 1.9 Problemas funcionais que afetam UX

Antes de qualquer polish visual, o redesign deve tratar:

- Falha no carregamento dos logs de IA que pode manter loading infinito.
- Falha ao carregar um monitor em edição que libera um formulário com valores padrão.
- Toast e confirmação globais ligados a uma raiz Angular que não é a efetivamente inicializada.
- Erros globais e locais gerando notificações duplicadas.
- Dashboard exibindo saúde positiva antes de confirmar os dados.
- Exclusão usando `window.confirm` apesar da existência de um componente próprio.

## 2. Direção de identidade proposta

### 2.1 Conceito

**Frontier intelligence:** um observador persistente que rastreia o mercado, separa ruído de oportunidade e explica por que um anúncio merece atenção.

Características da marca:

- Vigilante, não agressiva.
- Técnica, mas compreensível.
- Robusta, sem parecer antiquada.
- Bem-humorada na medida certa.
- Orientada a evidências e transparência.

### 2.2 Assinatura recomendada

```text
Bobão do Oeste
Monitor inteligente de oportunidades
```

Alternativa mais neutra, caso o produto precise de posicionamento corporativo:

```text
Marketplace Intelligence
Monitoramento e análise de oportunidades
```

Não misturar os dois nomes na navegação. O nome escolhido deve aparecer no título do documento, login, shell, favicon metadata, README e mensagens de sistema.

### 2.3 Paleta semântica inicial

| Token proposto | Valor inicial | Uso |
| :--- | :--- | :--- |
| `--brand-950` | `#0F0E0D` | Fundo mais escuro e texto de alto contraste |
| `--brand-900` | `#191715` | Sidebar, hero e superfícies escuras |
| `--brand-800` | `#262320` | Superfícies elevadas escuras |
| `--brand-amber` | `#F59E0B` | CTA sobre fundo escuro, seleção e destaques |
| `--brand-amber-strong` | `#D97706` | Hover e bordas de destaque |
| `--brand-brass` | `#D4A373` | Ornamentos, divisores e ícones de marca |
| `--brand-leather` | `#4A3423` | Detalhes e superfícies temáticas controladas |
| `--radar` | `#0891B2` | Links, telemetria e elementos de rastreamento |
| `--canvas` | `#F7F4EE` | Fundo claro da aplicação |
| `--surface` | `#FFFDF8` | Cards e formulários |
| `--ink` | `#1C1917` | Texto principal |

O âmbar não deve ser usado como texto comum sobre branco por falta de contraste. Ele funciona melhor como fundo com texto escuro, foco, borda ou elemento sobre superfícies escuras.

Cores semânticas de sucesso, aviso, erro e informação devem permanecer separadas das cores da marca.

### 2.4 Tipografia

Combinação recomendada:

- **Bitter** para títulos e momentos editoriais da marca.
- **IBM Plex Sans** para navegação, formulários, tabelas e textos.
- **IBM Plex Mono** para IDs, payloads, prompts e logs.

A fonte serifada não deve aparecer em controles densos. Ela serve para dar personalidade a títulos, hero, onboarding e empty states.

Escala mínima recomendada:

| Papel | Tamanho sugerido |
| :--- | :--- |
| Display | `36–48px` |
| H1 | `28–32px` |
| H2 | `22–24px` |
| H3 | `18–20px` |
| Corpo | `14–16px` |
| Apoio | `13–14px` |
| Badge e código | mínimo de `12px` |

### 2.5 Formas e superfícies

- Controles: `rounded-lg`, altura mínima de 40 px e 44 px no móvel.
- Cards: `rounded-xl`, borda visível e sombra curta.
- Dialogs: `rounded-2xl` apenas quando houver espaço suficiente.
- Badges: pill para status curtos; retângulo arredondado para dados extensos.
- Foco: ring de 2 px com alto contraste e offset perceptível.
- Ornamentos de faroeste: somente em separadores, empty states e áreas institucionais.

### 2.6 Imagens e símbolo

| Asset | Papel recomendado | Tratamento |
| :--- | :--- | :--- |
| `docs/gemini-svg.svg` | Fonte vetorial do símbolo | Criar no futuro versões completa, reduzida, clara e monocromática |
| `docs/Gemini_Generated_Image_r6nd1pr6nd1pr6nd.jpg` | Login, onboarding e hero | Converter para AVIF/WebP, aplicar recortes responsivos e manter texto fora da imagem |
| `docs/Gemini_Generated_Image_2oesiz2oesiz2oes.jpg` | Campanha e empty state narrativo | Avaliar uma versão sem arma para contextos amplos |
| `docs/Gemini_Generated_Image_bd2eylbd2eylbd2e.jpg` | Prancha de referência | Não usar como logo final por ser raster e conter duas marcas |

O favicon usa temporariamente o símbolo completo em `public/assets/brand/marketplace-intelligence-mark.svg`. Para 16 e 24 px, deve ser criada uma variante simplificada contendo apenas chapéu, chip e olhos.

### 2.7 Movimento

- Usar brilho/radar somente para ações em processamento ou descoberta recente.
- Limitar animações de entrada a 150–220 ms.
- Evitar pulse contínuo para mensagens de erro.
- Implementar variantes com `prefers-reduced-motion`.
- Nunca depender de animação para comunicar status.

## 3. Arquitetura de informação proposta

### Navegação principal

1. **Visão geral**: oportunidades recentes, alertas e resumo dos monitores.
2. **Oportunidades**: feed global de anúncios com filtros e ordenação.
3. **Monitores**: criação, configuração e saúde de cada monitor.

### Navegação operacional

1. **Execuções e eventos**: scraping, webhook e filas.
2. **Auditoria da IA**: prompts, respostas, custo e latência.

A seção operacional pode ser separada visualmente ou exibida somente para perfis administrativos. Isso mantém a navegação primária alinhada ao objetivo do usuário.

### Contexto de página

- Topbar deve refletir a rota atual, em vez de sempre mostrar "Dashboard Administrativo".
- Páginas internas devem possuir breadcrumb quando houver relação pai-filho.
- Tabs do detalhe precisam ser representadas na URL.
- Rotas desconhecidas devem apresentar uma página 404 real.
- Login deve preservar `returnUrl`.

## 4. Redesign por tela

### Login

- Layout dividido em desktop, com a imagem do robô analisando a GPU em um painel visual.
- Formulário em superfície sólida, sem texto sobre imagem.
- Símbolo e assinatura do produto acima do formulário.
- Labels, autocomplete, botão para revelar senha e erro por campo.
- Coluna visual removida ou recortada no móvel.
- Toast global disponível antes e depois da autenticação.

### Visão geral

- Hero compacto com a oportunidade mais relevante ou resumo do dia.
- Métricas com skeleton, erro individual e horário da última atualização.
- Lista de anúncios novos e de alto score.
- Resumo da saúde operacional em uma seção secundária.
- CTA claro para criar o primeiro monitor.
- Nunca mostrar "ocioso" antes de confirmar a resposta do scraper.

### Oportunidades

Nova tela global baseada no endpoint de listagem já existente:

- Busca textual.
- Filtros por monitor, marketplace, faixa de preço, score, tier, localização e entrega.
- Ordenação por relevância, novidade e preço.
- Cards no móvel e visualização mais densa no desktop.
- Favoritar, abrir anúncio e entender a justificativa da IA.
- Empty state diferente para "sem dados" e "nenhum resultado para estes filtros".

### Lista de monitores

- Busca e filtros por status, vendor e frequência.
- Cards compactos no móvel e tabela no desktop.
- Ações secundárias reunidas em menu contextual.
- Loading somente na linha que está sendo atualizada.
- Confirmação acessível para exclusão.
- Labels amigáveis em português no lugar de enums técnicos.

### Formulário de monitor

- Uma coluna no móvel e seções progressivas no desktop.
- Inputs com labels associados, ajuda e mensagens de validação.
- Palavras-chave representadas como chips.
- Validação cruzada de preço mínimo e máximo.
- Explicação do custo e da latência das estratégias de IA.
- Remover ou desabilitar opções sem implementação real.
- Preview da busca antes de salvar.
- Proteção contra saída com alterações não salvas.
- Estado bloqueante com retry quando a edição não carregar.

### Detalhe do monitor

- Cabeçalho responsivo com status, última coleta e ações.
- Resumo dos critérios antes das tabs.
- Imagens com `alt`, lazy loading, proporção fixa e fallback.
- Cards de anúncio no móvel.
- Filtros locais por preço, score, tier, localização e entrega.
- Logs de IA paginados e com erro recuperável.
- Configuração RAW restrita a uma visão técnica.

### Eventos e auditoria da IA

- Mover para a seção operacional.
- Filtros por status, período, monitor e modelo.
- Cópia rápida de IDs com confirmação discreta.
- Estados de webhook alinhados ao contrato real.
- Dialog acessível para payloads, prompts e respostas.
- Formatação de JSON, quebra de linhas e ação para baixar dados quando necessário.

## 5. Fundação técnica do design system

### Tokens

Centralizar em `apps/web/project-ui/src/styles.css` usando Tailwind 4 e variáveis CSS:

- Cores de marca e semânticas.
- Tipografia e escala.
- Espaçamento de página e componentes.
- Radius e sombras.
- Largura máxima do conteúdo.
- Alturas de controle.
- Focus ring.
- Durações e curvas de animação.

### Componentes prioritários

1. `UiPageHeader`
2. `UiFormField`
3. `UiInput` e `UiSelect`
4. `UiIconButton`
5. `UiStatePanel` para loading, empty e error
6. `UiDialog`
7. `UiStatusBadge`
8. `UiDataView` com tabela desktop e cards móveis
9. `UiCodePanel`
10. `UiSkeleton`

Componentes existentes devem ser consolidados, não duplicados. `UiButton`, `UiCard`, `UiBadge`, `UiTabs` e `UiPagination` podem evoluir sobre a base atual.

### Regras de adoção

- Nenhum botão local se existir uma variante compartilhada equivalente.
- Nenhuma cor de status definida diretamente em uma feature.
- Nenhum modal sem o componente acessível comum.
- Nenhuma chamada remota sem estado de loading, empty e error.
- Nenhum ícone sem nome acessível ou `aria-hidden` quando decorativo.
- Nenhum novo texto técnico sem tradução de apresentação.

## 6. Plano de execução

### Fase 0: decisão de marca e produto

Objetivo: eliminar ambiguidades antes de criar componentes e textos.

Ações:

- Confirmar o nome canônico e a assinatura.
- Definir persona principal e quais perfis acessam ferramentas operacionais.
- Aprovar ou rejeitar o uso da imagem com arma.
- Criar glossário de vendors, frequências, análises e status.
- Registrar a proveniência e permissão de uso dos assets gerados.

Critério de aceite: uma página curta de brand brief aprovada e sem nomes concorrentes.

### Fase 1: correções funcionais e acessibilidade

Objetivo: remover riscos antes do redesign visual.

Ações:

- Corrigir a raiz de toast e confirmação.
- Substituir o menu hover por um menu operável por botão e teclado.
- Criar dialog com foco completo.
- Associar labels e mensagens aos campos.
- Corrigir loading infinito e edição após falha.
- Separar erro de vazio em todas as consultas.
- Adicionar live regions e foco visível.
- Configurar locale `pt-BR` para datas e moeda.

Critério de aceite: login, monitor, menu e dialogs operáveis somente por teclado, sem erro silencioso.

### Fase 2: tokens e componentes

Objetivo: construir uma base reutilizável antes de redesenhar telas.

Ações:

- Implementar os tokens propostos.
- Adicionar as famílias tipográficas aprovadas.
- Consolidar os componentes compartilhados.
- Remover sintaxe legada de opacidade do Tailwind.
- Criar documentação visual dos estados e variantes.

Critério de aceite: cada primitive possui variantes, foco, disabled, loading e contraste validados.

### Fase 3: shell e responsividade

Objetivo: aplicar a marca e criar navegação confiável em todos os viewports.

Ações:

- Aplicar símbolo na sidebar, topbar móvel e login.
- Atualizar nomes e títulos por rota.
- Reorganizar navegação em produto e operação.
- Usar `100dvh`, safe areas e `viewport-fit=cover`.
- Criar cabeçalhos responsivos e padrões de conteúdo.
- Implementar 404 e breadcrumbs.

Critério de aceite: nenhuma página gera overflow horizontal em 320 px e a navegação preserva contexto.

### Fase 4: telas de valor do produto

Objetivo: colocar oportunidades no centro da experiência.

Ações:

- Redesenhar login e dashboard.
- Criar a tela global de oportunidades.
- Redesenhar lista, formulário e detalhe de monitores.
- Adicionar filtros, ordenação e estados contextualizados.
- Usar as imagens aprovadas em login, onboarding e empty states.

Critério de aceite: uma oportunidade relevante pode ser encontrada e compreendida sem navegar por telas técnicas.

### Fase 5: ferramentas operacionais

Objetivo: manter profundidade técnica sem poluir a experiência principal.

Ações:

- Redesenhar eventos e auditoria da IA.
- Adicionar filtros e cópia de IDs.
- Melhorar visualização de JSON e prompts.
- Alinhar status e labels aos contratos reais.

Critério de aceite: falhas de scraping, webhook e IA podem ser diagnosticadas sem sair da SPA.

### Fase 6: validação contínua

Objetivo: impedir regressões visuais e de acessibilidade.

Ações:

- Adicionar testes Vitest aos componentes compartilhados.
- Adicionar Playwright aos fluxos críticos.
- Integrar axe para acessibilidade.
- Criar testes de regressão visual.
- Executar testes em 320, 375, 768, 1024 e 1440 px.
- Validar zoom em 200%, teclado e movimento reduzido.
- Incluir testes, e não apenas build, no workflow do frontend.

Critério de aceite: CI bloqueia regressões funcionais, acessíveis e visuais críticas.

## 7. Prioridades recomendadas

| Prioridade | Trabalho | Motivo |
| :--- | :--- | :--- |
| P0 | Erro de edição, loading infinito, root de overlays, labels, menu e dialogs | Pode causar perda de dados ou bloquear usuários |
| P1 | Estados remotos, dashboard confiável, responsividade e foco | Afeta confiança e uso diário |
| P1 | Tokens, componentes e terminologia | Evita ampliar inconsistências durante o redesign |
| P2 | Nova arquitetura de informação e tela de oportunidades | Alinha a SPA ao valor do produto |
| P2 | Aplicação completa da marca e ilustrações | Cria diferenciação sem comprometer a base |
| P3 | Dark mode, gráficos avançados e microinterações | Só deve entrar após os fluxos principais estarem sólidos |

## 8. Definition of Done

O redesign pode ser considerado concluído quando:

- Existe um único nome de produto em toda a SPA.
- A navegação prioriza visão geral, oportunidades e monitores.
- Todas as requisições têm loading, vazio e erro distinguíveis.
- Nenhuma ação essencial depende de hover.
- Todos os formulários têm labels e erros acessíveis.
- Todos os dialogs gerenciam foco e Escape.
- Nenhuma página possui overflow horizontal no viewport móvel.
- Datas, moeda e textos usam `pt-BR`.
- Cores e componentes vêm de tokens compartilhados.
- O contraste atende WCAG AA nos fluxos essenciais.
- `prefers-reduced-motion` é respeitado.
- O frontend possui testes de componentes, E2E e acessibilidade na CI.
- Assets raster são entregues em formatos e tamanhos responsivos.

## 9. Decisões originais (resolvidas)

As respostas estão consolidadas em "Decisões confirmadas", no topo deste documento. Registro original das perguntas:

1. O nome público será `Bobão do Oeste` ou `Marketplace Intelligence`?
2. A imagem do caçador armado pode aparecer no produto ou ficará restrita ao README?
3. A persona principal é comprador/analista, operador técnico ou ambas?
4. Eventos e auditoria serão visíveis para todos ou apenas administradores?
5. Dark mode faz parte da primeira entrega ou de uma fase posterior?
6. Os assets gerados possuem autorização definitiva para uso público e comercial?
