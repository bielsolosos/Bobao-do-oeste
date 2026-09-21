import { Component, input, output } from '@angular/core';

type StateVariant = 'loading' | 'empty' | 'error';

@Component({
  selector: 'app-ui-state-panel',
  standalone: true,
  template: `
    <div
      class="flex flex-col items-center justify-center gap-3 px-6 py-14 text-center"
      [attr.role]="variant() === 'error' ? 'alert' : 'status'"
      [attr.aria-busy]="variant() === 'loading' ? 'true' : null"
    >
      @switch (variant()) {
        @case ('loading') {
          <svg
            class="h-8 w-8 animate-spin text-brand-amber-strong"
            viewBox="0 0 24 24"
            fill="none"
            aria-hidden="true"
          >
            <circle
              class="opacity-25"
              cx="12"
              cy="12"
              r="10"
              stroke="currentColor"
              stroke-width="4"
            ></circle>
            <path
              class="opacity-75"
              fill="currentColor"
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
            ></path>
          </svg>
        }
        @case ('error') {
          <div
            class="flex h-12 w-12 items-center justify-center rounded-full bg-red-100 text-red-600"
            aria-hidden="true"
          >
            <svg
              class="h-6 w-6"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="1.5"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.008v.008H12v-.008z"
              />
            </svg>
          </div>
        }
        @default {
          <div
            class="flex h-12 w-12 items-center justify-center rounded-full bg-brand-950/5 text-brand-950/40"
            aria-hidden="true"
          >
            <svg
              class="h-6 w-6"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="1.5"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z"
              />
            </svg>
          </div>
        }
      }

      <div class="max-w-md">
        <h3 class="text-sm font-semibold text-brand-950">{{ title() }}</h3>
        @if (message()) {
          <p class="mt-1 text-sm text-brand-950/60">{{ message() }}</p>
        }
      </div>

      @if (variant() === 'error') {
        <button
          type="button"
          (click)="retry.emit()"
          class="mt-1 inline-flex items-center gap-2 rounded-lg bg-brand-amber-strong px-4 py-2 text-sm font-medium text-brand-950 transition-colors hover:bg-brand-amber"
        >
          <svg
            class="h-4 w-4"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            stroke-width="2"
            aria-hidden="true"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
            />
          </svg>
          Tentar novamente
        </button>
      }
    </div>
  `,
})
export class UiStatePanelComponent {
  readonly variant = input<StateVariant>('loading');
  readonly title = input<string>('');
  readonly message = input<string>('');
  readonly retry = output<void>();
}
