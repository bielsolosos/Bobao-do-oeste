import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-ui-empty-state',
  standalone: true,
  template: `
    <div class="flex flex-col items-center justify-center px-6 py-16 text-center">
      <div class="flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-100 text-slate-400">
        <svg class="h-7 w-7" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 13V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v7m16 0v5a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-5m16 0h-2.586a1 1 0 0 0-.707.293l-2.414 2.414a1 1 0 0 1-.707.293h-3.172a1 1 0 0 1-.707-.293l-2.414-2.414A1 1 0 0 0 6.586 13H4" />
        </svg>
      </div>
      <h2 class="mt-4 text-sm font-semibold text-slate-900">{{ title }}</h2>
      @if (message) {
        <p class="mt-1 max-w-md text-sm text-slate-500">{{ message }}</p>
      }
      <div class="mt-5">
        <ng-content></ng-content>
      </div>
    </div>
  `,
})
export class UiEmptyStateComponent {
  @Input({ required: true }) title = '';
  @Input() message = '';
}
