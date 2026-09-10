import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ui-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button
      [type]="type"
      [disabled]="disabled || loading"
      (click)="clicked.emit($event)"
      [ngClass]="getButtonClasses()"
      class="inline-flex items-center justify-center font-medium rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
    >
      @if (loading) {
        <svg
          class="animate-spin -ml-1 mr-2 h-4 w-4"
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle
            class="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            stroke-width="4"
          ></circle>
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
  @Input() variant: 'primary' | 'secondary' | 'danger' | 'outline' | 'ghost' = 'primary';
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  @Input() loading: boolean = false;
  @Input() disabled: boolean = false;
  @Input() fullWidth: boolean = false;

  @Output() clicked = new EventEmitter<Event>();

  getButtonClasses(): string {
    let classes = '';

    // Size
    switch (this.size) {
      case 'sm':
        classes += 'px-3 py-1.5 text-xs ';
        break;
      case 'md':
        classes += 'px-4 py-2 text-sm ';
        break;
      case 'lg':
        classes += 'px-6 py-3 text-base ';
        break;
    }

    // Width
    if (this.fullWidth) {
      classes += 'w-full ';
    }

    // Variant
    switch (this.variant) {
      case 'primary':
        classes +=
          'bg-blue-600 text-white hover:bg-blue-700 focus:ring-blue-500 border border-transparent';
        break;
      case 'secondary':
        classes +=
          'bg-gray-100 text-gray-800 hover:bg-gray-200 focus:ring-gray-500 border border-transparent';
        break;
      case 'danger':
        classes +=
          'bg-red-600 text-white hover:bg-red-700 focus:ring-red-500 border border-transparent';
        break;
      case 'outline':
        classes +=
          'bg-transparent text-gray-700 border border-gray-300 hover:bg-gray-50 focus:ring-gray-500';
        break;
      case 'ghost':
        classes +=
          'bg-transparent text-gray-600 hover:bg-gray-100 hover:text-gray-900 focus:ring-gray-500 border border-transparent';
        break;
    }

    return classes;
  }
}
