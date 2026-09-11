import { Component, Input, computed, signal } from '@angular/core';

@Component({
  selector: 'app-ui-skeleton',
  standalone: true,
  template: `
    @if (variant() === 'table') {
      <div class="divide-y divide-brand-950/10" role="status" aria-busy="true" aria-label="Carregando">
        @for (row of rowsArray(); track $index) {
          <div class="flex items-center gap-4 px-6 py-5">
            <div class="h-10 w-10 shrink-0 animate-pulse rounded-lg bg-brand-950/10"></div>
            <div class="min-w-0 flex-1 space-y-2">
              <div class="h-3 w-2/5 animate-pulse rounded bg-brand-950/10"></div>
              <div class="h-2.5 w-1/4 animate-pulse rounded bg-brand-950/5"></div>
            </div>
            <div class="hidden h-3 w-20 animate-pulse rounded bg-brand-950/10 sm:block"></div>
            <div class="h-8 w-24 animate-pulse rounded bg-brand-950/5"></div>
          </div>
        }
      </div>
    } @else {
      <div class="animate-pulse space-y-2" role="status" aria-busy="true" aria-label="Carregando">
        @for (line of rowsArray(); track $index) {
          <div
            class="h-3 rounded-full bg-brand-950/10"
            [class]="$last && rowsArray().length > 1 ? 'w-2/3' : 'w-full'"
          ></div>
        }
      </div>
    }
  `,
})
export class UiSkeletonComponent {
  private readonly variantInput = signal<'table' | 'lines'>('lines');
  private readonly linesInput = signal(1);

  @Input() set variant(v: 'table' | 'lines') {
    this.variantInput.set(v ?? 'lines');
  }
  @Input() set rows(value: number) {
    this.linesInput.set(value ?? 1);
  }
  @Input() set lines(value: number) {
    this.linesInput.set(value ?? 1);
  }

  readonly variant = this.variantInput.asReadonly();

  rowsArray = computed(() => Array.from({ length: this.linesInput() }));
}
