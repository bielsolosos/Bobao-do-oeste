import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [RouterModule],
  template: `
    <div
      class="flex min-h-screen flex-col items-center justify-center gap-4 bg-canvas px-6 text-center"
    >
      <img
        src="assets/brand/marketplace-intelligence-mark.svg"
        alt=""
        class="h-20 w-20 opacity-90"
        aria-hidden="true"
      />
      <p class="page-eyebrow">Erro 404</p>
      <h1 class="font-display text-3xl font-bold text-brand-950">Trilha perdida no deserto</h1>
      <p class="max-w-md text-sm text-brand-950/60">
        A página que você tentou acessar não existe ou foi movida. Volte para a visão geral para
        continuar a caçada.
      </p>
      <a
        routerLink="/dashboard"
        class="mt-2 inline-flex items-center gap-2 rounded-lg bg-brand-amber-strong px-5 py-2.5 text-sm font-medium text-brand-950 transition-colors hover:bg-brand-amber"
      >
        Voltar para a visão geral
      </a>
    </div>
  `,
})
export class NotFoundComponent {}
