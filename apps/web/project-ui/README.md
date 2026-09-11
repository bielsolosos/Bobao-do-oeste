# Bobão do Oeste — SPA

Interface web do Marketplace Intelligence Ecosystem. Um painel Angular para monitorar anúncios, revisar oportunidades priorizadas pela IA e operar a infraestrutura de scraping.

## Stack

- Angular 22 com componentes standalone e signals.
- TypeScript 6, RxJS e Angular Router.
- Tailwind CSS 4 com tokens próprios em `src/styles.css`.
- Vitest para testes unitários.
- Nginx para servir o build estático.

## Pré-requisitos

- Node.js 24 e npm 11.
- BI Engine em execução para dados reais (padrão `http://localhost:8080/api/v1`).

## Desenvolvimento

```bash
npm install
npm start
```

A aplicação estará em `http://localhost:4200`. A URL da API pode ser sobrescrita em runtime por `assets/env.js` ou pela variável `API_URL` no container.

## Estrutura

```text
src/app
├── core           Modelos, services, guards e interceptors
├── features       Login, dashboard, oportunidades, monitores, eventos e IA
├── layout         Shell autenticado com sidebar e navegação móvel
└── shared         Design system e componentes reutilizáveis
```

## Identidade

A marca combina hardware, inteligência artificial e a metáfora de um caçador de oportunidades. O emblema vetorial fica em `public/assets/brand/marketplace-intelligence-mark.svg` e é usado como favicon e símbolo da interface.

## Testes e build

```bash
npm test
npm run build
```

## Acessibilidade

- Formulários com labels associados e mensagens de erro acessíveis.
- Dialogs com focus trap, fechamento por Escape e restauração de foco.
- Navegação por teclado no menu, tabs e paginação.
- `prefers-reduced-motion` respeitado.
- Locale `pt-BR` para datas e moeda.

## Documentação

- [Plano de identidade visual e redesign](../../docs/UI_UX_REDESIGN_PLAN.md)
- [README raiz](../../README.md)
