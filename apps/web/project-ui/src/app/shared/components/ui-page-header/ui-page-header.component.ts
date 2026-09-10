import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-ui-page-header',
  standalone: true,
  template: `
    <header class="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 class="text-2xl font-bold tracking-tight text-slate-900">{{ title }}</h1>
        @if (subtitle) {
          <p class="mt-1 text-sm text-slate-500">{{ subtitle }}</p>
        }
      </div>
      <div class="flex items-center gap-2 sm:shrink-0">
        <ng-content></ng-content>
      </div>
    </header>
  `,
})
export class UiPageHeaderComponent {
  @Input({ required: true }) title = '';
  @Input() subtitle = '';
}
