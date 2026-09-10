import { Component, inject } from '@angular/core';

import { UiToastService } from './ui-toast.service';

@Component({
  selector: 'app-ui-toast-container',
  standalone: true,
  imports: [],
  template: `
    <!-- Ajustado para ficar no TOPO e com largura maior (w-96) -->
    <div
      class="fixed top-6 right-6 z-[var(--z-toast)] flex flex-col gap-3 pointer-events-none w-96 max-w-[90vw]"
    >
      @for (toast of toastService.toasts(); track toast.id) {
        <div
          class="pointer-events-auto w-full bg-white shadow-xl rounded-lg ring-1 ring-black/5 overflow-hidden transition-all flex items-start p-4 animate-slide-in"
        >
          <div class="flex-shrink-0 mt-0.5">
            @if (toast.type === 'success') {
              <svg
                class="h-6 w-6 text-green-500"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
            }
            @if (toast.type === 'error') {
              <svg
                class="h-6 w-6 text-red-500"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
            }
            @if (toast.type === 'warning') {
              <svg
                class="h-6 w-6 text-yellow-500"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                />
              </svg>
            }
            @if (toast.type === 'info') {
              <svg
                class="h-6 w-6 text-blue-500"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
            }
          </div>
          <div class="ml-3 w-0 flex-1">
            <p class="text-sm font-semibold text-gray-900">{{ toast.title }}</p>
            @if (toast.message) {
              <p class="mt-1 text-sm text-gray-600 leading-snug">{{ toast.message }}</p>
            }
          </div>
          <div class="ml-4 flex-shrink-0 flex">
            <button
              type="button"
              (click)="toastService.remove(toast.id)"
              class="bg-white rounded-md inline-flex text-gray-400 hover:text-gray-600 hover:bg-gray-100 p-1 transition-colors focus:outline-none"
            >
              <span class="sr-only">Fechar</span>
              <svg class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
                <path
                  fill-rule="evenodd"
                  d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                  clip-rule="evenodd"
                />
              </svg>
            </button>
          </div>
        </div>
      }
    </div>
  `,
  styles: [
    `
      .animate-slide-in {
        animation: slideInRight 0.4s cubic-bezier(0.16, 1, 0.3, 1) forwards;
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
