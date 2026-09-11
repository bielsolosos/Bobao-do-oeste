import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'outline' | 'ghost';

@Component({
  selector: 'app-ui-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button
      [type]="type"
      [disabled]="disabled || loading"
      [attr.aria-busy]="loading ? 'true' : null"
      (click)="emitClick($event)"
      [ngClass]="getButtonClasses()"
      class="inline-flex items-center justify-center rounded-lg font-medium transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
    >
      @if (loading) {
        <svg
          class="mr-2 h-4 w-4 animate-spin"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
          aria-hidden="true"
        >
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path
            class="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
          ></path>
        </svg>
      }

      <ng-content></ng-content>
    </button>
  `,
})
export class UiButtonComponent {
  @Input() type: 'button' | 'submit' | 'reset' = 'button';
  @Input() variant: ButtonVariant = 'primary';
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  @Input() loading: boolean = false;
  @Input() disabled: boolean = false;
  @Input() fullWidth: boolean = false;

  @Output() clicked = new EventEmitter<Event>();
  @Output() pressed = new EventEmitter<Event>();

  emitClick(event: Event) {
    this.clicked.emit(event);
    this.pressed.emit(event);
  }

  getButtonClasses(): string {
    const sizes: Record<string, string> = {
      sm: 'px-3 py-1.5 text-xs ',
      md: 'min-h-10 px-4 py-2 text-sm ',
      lg: 'min-h-11 px-6 py-3 text-base ',
    };

    const variants: Record<ButtonVariant, string> = {
      primary:
        'bg-brand-amber-strong text-brand-950 hover:bg-brand-amber focus-visible:outline-brand-amber-strong border border-transparent',
      secondary:
        'bg-brand-950/5 text-brand-950 hover:bg-brand-950/10 focus-visible:outline-brand-700 border border-transparent',
      danger: 'bg-red-600 text-white hover:bg-red-700 focus-visible:outline-red-600 border border-transparent',
      outline:
        'bg-surface text-brand-950 border border-brand-950/20 hover:bg-brand-950/5 focus-visible:outline-brand-700',
      ghost:
        'bg-transparent text-brand-950/70 hover:bg-brand-950/5 hover:text-brand-950 focus-visible:outline-brand-700 border border-transparent',
    };

    return (sizes[this.size] ?? sizes['md']) + (this.fullWidth ? 'w-full ' : '') + variants[this.variant];
  }
}
