import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-ui-skeleton',
  standalone: true,
  template: `
    <div class="animate-pulse space-y-2" aria-hidden="true">
      @for (line of rows(); track $index) {
        <div
          class="h-3 rounded-full bg-brand-950/10"
          [class]="$last && rows().length > 1 ? 'w-2/3' : 'w-full'"
        ></div>
      }
    </div>
  `,
})
export class UiSkeletonComponent {
  readonly lines = input(1);
  readonly rows = computed(() => Array.from({ length: this.lines() }));
}
