import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-ui-skeleton',
  standalone: true,
  template: `
    @if (variant === 'table') {
      <div class="divide-y divide-slate-100" aria-label="Carregando" aria-busy="true">
        @for (row of rowsArray; track $index) {
          <div class="flex items-center gap-4 px-6 py-5">
            <div class="h-10 w-10 shrink-0 animate-pulse rounded-lg bg-slate-200"></div>
            <div class="min-w-0 flex-1 space-y-2">
              <div class="h-3 w-2/5 animate-pulse rounded bg-slate-200"></div>
              <div class="h-2.5 w-1/4 animate-pulse rounded bg-slate-100"></div>
            </div>
            <div class="hidden h-3 w-20 animate-pulse rounded bg-slate-200 sm:block"></div>
            <div class="h-8 w-24 animate-pulse rounded bg-slate-100"></div>
          </div>
        }
      </div>
    } @else {
      <div class="space-y-3" aria-label="Carregando" aria-busy="true">
        @for (row of rowsArray; track $index) {
          <div
            class="h-4 w-full animate-pulse rounded bg-slate-200"
            [class.w-3\/4]="$index % 2 === 0"
          ></div>
        }
      </div>
    }
  `,
})
export class UiSkeletonComponent {
  @Input() variant: 'table' | 'lines' = 'lines';
  @Input() rows = 5;

  get rowsArray(): number[] {
    return Array.from({ length: this.rows }, (_, index) => index);
  }
}
