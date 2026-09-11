import { Component, inject } from '@angular/core';

import { UiToastService } from './ui-toast.service';

@Component({
  selector: 'app-ui-toast-container',
  standalone: true,
  imports: [],
  template: `
    <div
      class="pointer-events-none fixed inset-x-3 top-3 z-[var(--z-toast)] flex flex-col gap-3 sm:inset-x-auto sm:right-6 sm:top-6 sm:w-96 sm:max-w-[90vw]"
      aria-live="polite"
      aria-relevant="additions"
    >
      @for (toast of toastService.toasts(); track toast.id) {
        <div
          [attr.role]="toast.type === 'error' ? 'alert' : 'status'"
          class="pointer-events-auto flex w-full items-start overflow-hidden rounded-xl bg-surface p-4 shadow-[var(--shadow-pop)] ring-1 ring-brand-950/10 animate-slide-in"
        >
          <div class="mt-0.5 flex-shrink-0" aria-hidden="true">
            @switch (toast.type) {
              @case ('success') {
                <svg class="h-6 w-6 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              }
              @case ('error') {
                <svg class="h-6 w-6 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              }
              @case ('warning') {
                <svg class="h-6 w-6 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
              }
              @case ('info') {
                <svg class="h-6 w-6 text-radar" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              }
            }
          </div>
          <div class="ml-3 w-0 flex-1">
            <p class="text-sm font-semibold text-brand-950">{{ toast.title }}</p>
            @if (toast.message) {
              <p class="mt-1 text-sm leading-snug text-brand-950/60">{{ toast.message }}</p>
            }
          </div>
          <button
            type="button"
            (click)="toastService.remove(toast.id)"
            class="ml-3 flex-shrink-0 rounded-md p-1 text-brand-950/40 transition-colors hover:bg-brand-950/5 hover:text-brand-950"
          >
            <span class="sr-only">Fechar notificação</span>
            <svg class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
              <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd" />
            </svg>
          </button>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .animate-slide-in {
        animation: slideInRight var(--duration-base) var(--ease-out-soft) forwards;
      }
      @keyframes slideInRight {
        from {
          transform: translateX(100%);
          opacity: 0;
        }
        to {
          transform: translateX(0);
          opacity: 1;
        }
      }
    `,
  ],
})
export class UiToastComponent {
  toastService = inject(UiToastService);
}
