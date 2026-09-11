import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ui-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section
      class="overflow-hidden rounded-xl border border-brand-950/10 bg-surface shadow-[var(--shadow-card)]"
      [ngClass]="extraClasses"
    >
      @if (title || subtitle || hasHeaderAction) {
        <header
          class="flex flex-wrap items-center justify-between gap-3 border-b border-brand-950/10 bg-brand-950/[0.02] px-5 py-4 sm:px-6"
        >
          <div>
            @if (title) {
              <h3 class="text-base font-semibold text-brand-950">{{ title }}</h3>
            }
            @if (subtitle) {
              <p class="mt-0.5 text-sm text-brand-950/60">{{ subtitle }}</p>
            }
          </div>
          @if (hasHeaderAction) {
            <div>
              <ng-content select="[card-action]"></ng-content>
            </div>
          }
        </header>
      }

      <div [ngClass]="noPadding ? '' : 'p-5 sm:p-6'">
        <ng-content></ng-content>
      </div>

      @if (hasFooter) {
        <footer class="border-t border-brand-950/10 bg-brand-950/[0.02] px-5 py-4 sm:px-6">
          <ng-content select="[card-footer]"></ng-content>
        </footer>
      }
    </section>
  `,
})
export class UiCardComponent {
  @Input() title?: string;
  @Input() subtitle?: string;
  @Input() noPadding: boolean = false;
  @Input() hasHeaderAction: boolean = false;
  @Input() hasFooter: boolean = false;
  @Input() extraClasses: string = '';
}
