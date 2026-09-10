import {
  AfterViewChecked,
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  Output,
  ViewChild,
  inject,
} from '@angular/core';
import { FocusTrap, FocusTrapFactory } from '@angular/cdk/a11y';

@Component({
  selector: 'app-ui-modal',
  standalone: true,
  template: `
    @if (open) {
      <div
        class="fixed inset-0 z-[var(--z-overlay)] flex items-center justify-center p-4"
        role="presentation"
        tabindex="-1"
        (click)="onBackdropClick($event)"
        (keydown.enter)="close()"
        (keydown.space)="close()"
      >
        <div class="absolute inset-0 bg-slate-950/55 backdrop-blur-sm"></div>
        <section
          #dialog
          class="relative flex max-h-[min(90vh,52rem)] w-full flex-col overflow-hidden rounded-2xl bg-white shadow-2xl"
          [class.max-w-lg]="size === 'sm'"
          [class.max-w-2xl]="size === 'md'"
          [class.max-w-5xl]="size === 'lg'"
          role="dialog"
          aria-modal="true"
          [attr.aria-labelledby]="titleId"
          tabindex="-1"
        >
          <header class="flex shrink-0 items-start justify-between gap-4 border-b border-slate-200 px-5 py-4 sm:px-6">
            <div>
              <h2 [id]="titleId" class="text-lg font-semibold text-slate-900">{{ title }}</h2>
              @if (subtitle) {
                <p class="mt-1 text-sm text-slate-500">{{ subtitle }}</p>
              }
            </div>
            <button
              type="button"
              class="shrink-0 rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
              aria-label="Fechar janela"
              (click)="close()"
            >
              <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M6 18 18 6M6 6l12 12" />
              </svg>
            </button>
          </header>

          <div class="app-scrollbar min-h-0 flex-1 overflow-y-auto px-5 py-5 sm:px-6">
            <ng-content></ng-content>
          </div>

        </section>
      </div>
    }
  `,
})
export class UiModalComponent implements AfterViewChecked {
  @Input() open = false;
  @Input({ required: true }) title = '';
  @Input() subtitle = '';
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  @Output() closed = new EventEmitter<void>();

  @ViewChild('dialog') dialog?: ElementRef<HTMLElement>;

  private readonly focusTrapFactory = inject(FocusTrapFactory);
  private focusTrap?: FocusTrap;
  private previouslyFocused: HTMLElement | null = null;
  readonly titleId = `ui-modal-title-${Math.random().toString(36).slice(2)}`;

  ngAfterViewChecked(): void {
    if (this.open && this.dialog && !this.focusTrap) {
      this.previouslyFocused = document.activeElement as HTMLElement | null;
      this.focusTrap = this.focusTrapFactory.create(this.dialog.nativeElement);
      this.focusTrap.focusInitialElementWhenReady();
    }

    if (!this.open && this.focusTrap) {
      this.focusTrap.destroy();
      this.focusTrap = undefined;
      this.previouslyFocused?.focus();
      this.previouslyFocused = null;
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open) this.close();
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) this.close();
  }

  close(): void {
    this.closed.emit();
  }
}
