import { Component, inject, input, signal } from '@angular/core';

import { UiToastService } from '../ui-toast/ui-toast.service';

@Component({
  selector: 'app-ui-code-panel',
  standalone: true,
  template: `
    <div class="overflow-hidden rounded-xl border border-brand-950/15 bg-brand-950">
      <div class="flex items-center justify-between gap-3 border-b border-white/10 px-4 py-2">
        <span class="text-xs font-medium uppercase tracking-wider text-white/60">{{
          label()
        }}</span>
        <button
          type="button"
          (click)="copy()"
          class="inline-flex items-center gap-1.5 rounded-md px-2 py-1 text-xs font-medium text-white/70 transition-colors hover:bg-white/10 hover:text-white"
        >
          @if (copied()) {
            <svg
              class="h-3.5 w-3.5"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
            </svg>
            Copiado
          } @else {
            <svg
              class="h-3.5 w-3.5"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="2"
              aria-hidden="true"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"
              />
            </svg>
            Copiar
          }
        </button>
      </div>
      <pre
        class="max-h-96 overflow-auto whitespace-pre-wrap p-4 font-mono text-[12px] leading-relaxed text-emerald-300"
        >{{ content() }}</pre>
    </div>
  `,
})
export class UiCodePanelComponent {
  private toast = inject(UiToastService);

  readonly label = input('Conteúdo');
  readonly content = input('');

  readonly copied = signal(false);

  async copy() {
    const text = this.content();
    if (!text) return;

    try {
      await navigator.clipboard.writeText(text);
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000); // CRÍTICOOOOOOOOOOOOO REVISARRRRRRRRRRRRRRRR
    } catch {
      this.toast.error('Não foi possível copiar', 'Copie o conteúdo manualmente.');
    }
  }
}
