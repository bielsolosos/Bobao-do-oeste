import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ui-icon-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button
      type="button"
      [attr.aria-label]="label"
      [title]="label"
      [disabled]="disabled"
      (click)="onClick.emit($event)"
      [ngClass]="toneClasses()"
      class="inline-flex h-9 w-9 items-center justify-center rounded-lg transition-colors disabled:cursor-not-allowed disabled:opacity-40"
    >
      <ng-content></ng-content>
    </button>
  `,
})
export class UiIconButtonComponent {
  @Input() label: string = '';
  @Input() tone: 'neutral' | 'danger' | 'warning' | 'success' = 'neutral';
  @Input() disabled = false;

  @Output() onClick = new EventEmitter<Event>();

  toneClasses(): string {
    switch (this.tone) {
      case 'danger':
        return 'text-red-600 hover:bg-red-50';
      case 'warning':
        return 'text-amber-700 hover:bg-amber-50';
      case 'success':
        return 'text-emerald-700 hover:bg-emerald-50';
      default:
        return 'text-brand-950/60 hover:bg-brand-950/5 hover:text-brand-950';
    }
  }
}
