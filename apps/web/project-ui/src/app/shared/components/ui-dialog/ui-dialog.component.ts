import {
  Component,
  ElementRef,
  ViewChild,
  effect,
  inject,
  input,
  output,
} from '@angular/core';
import { DOCUMENT } from '@angular/common';

const FOCUSABLE_SELECTOR =
  'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])';

@Component({
  selector: 'app-ui-dialog',
  standalone: true,
  template: `
    @if (open()) {
      <div
        class="fixed inset-0 z-[100] overflow-y-auto"
        role="dialog"
        aria-modal="true"
        [attr.aria-label]="ariaLabel() || null"
        [attr.aria-labelledby]="ariaLabel() ? null : titleId"
      >
        <div
          class="fixed inset-0 bg-brand-950/70 backdrop-blur-sm transition-opacity"
          (click)="requestClose()"
          aria-hidden="true"
        ></div>

        <div class="flex min-h-full items-end justify-center p-3 text-center sm:items-center sm:p-6">
          <div
            #panel
            class="relative w-full rounded-2xl bg-surface text-left shadow-[var(--shadow-pop)] outline-none"
            [class]="panelClass()"
            tabindex="-1"
          >
            @if (title()) {
              <div class="flex items-start justify-between gap-4 border-b border-brand-950/10 px-5 py-4 sm:px-6">
                <div>
                  <h2 [id]="titleId" class="text-lg font-semibold text-brand-950">{{ title() }}</h2>
                  @if (subtitle()) {
                    <p class="mt-0.5 text-sm text-brand-950/60">{{ subtitle() }}</p>
                  }
                </div>
                <button
                  type="button"
                  (click)="requestClose()"
                  class="rounded-md p-1.5 text-brand-950/50 transition-colors hover:bg-brand-950/5 hover:text-brand-950"
                >
                  <span class="sr-only">Fechar</span>
                  <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            }

            <div [class]="bodyClass()">
              <ng-content></ng-content>
            </div>
          </div>
        </div>
      </div>
    }
  `,
})
export class UiDialogComponent {
  private document = inject(DOCUMENT);

  readonly open = input(false);
  readonly title = input<string>('');
  readonly subtitle = input<string>('');
  readonly ariaLabel = input<string>('');
  readonly panelClass = input('sm:max-w-2xl');
  readonly bodyClass = input('px-5 py-5 sm:px-6');

  readonly closed = output<void>();

  @ViewChild('panel') panel?: ElementRef<HTMLElement>;

  readonly titleId = 'ui-dialog-title';

  private previouslyFocused: HTMLElement | null = null;
  private keydownHandler = (event: KeyboardEvent) => this.onKeydown(event);

  constructor() {
    effect((onCleanup) => {
      if (this.open()) {
        this.previouslyFocused = this.document.activeElement as HTMLElement | null;
        this.document.body.style.overflow = 'hidden';
        this.document.addEventListener('keydown', this.keydownHandler, true);
        queueMicrotask(() => this.focusFirst());

        onCleanup(() => {
          this.document.removeEventListener('keydown', this.keydownHandler, true);
          this.document.body.style.overflow = '';
          this.previouslyFocused?.focus?.();
        });
      }
    });
  }

  requestClose() {
    this.closed.emit();
  }

  private focusFirst() {
    const panel = this.panel?.nativeElement;
    if (!panel) return;
    const focusable = panel.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR);
    (focusable[0] ?? panel).focus();
  }

  private onKeydown(event: KeyboardEvent) {
    if (!this.open()) return;

    if (event.key === 'Escape') {
      event.preventDefault();
      this.requestClose();
      return;
    }

    if (event.key !== 'Tab') return;

    const panel = this.panel?.nativeElement;
    if (!panel) return;

    const focusable = Array.from(panel.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR)).filter(
      (el) => el.offsetParent !== null,
    );
    if (focusable.length === 0) {
      event.preventDefault();
      panel.focus();
      return;
    }

    const first = focusable[0];
    const last = focusable[focusable.length - 1];
    const active = this.document.activeElement;

    if (event.shiftKey && active === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && active === last) {
      event.preventDefault();
      first.focus();
    }
  }
}
