import { Component, input } from '@angular/core';

@Component({
  selector: 'app-ui-page-header',
  standalone: true,
  template: `
    <div class="mb-6 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div class="min-w-0">
        @if (eyebrow()) {
          <p class="page-eyebrow mb-1">{{ eyebrow() }}</p>
        }
        <h1 class="text-2xl font-bold tracking-tight text-brand-950 sm:text-[28px]">
          {{ title() }}
        </h1>
        @if (subtitle()) {
          <p class="mt-1 text-sm text-brand-950/60">{{ subtitle() }}</p>
        }
      </div>
      <div class="flex flex-wrap items-center gap-2 md:justify-end">
        <ng-content></ng-content>
      </div>
    </div>
  `,
})
export class UiPageHeaderComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string>('');
  readonly eyebrow = input<string>('');
}
