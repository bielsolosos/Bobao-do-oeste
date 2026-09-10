import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ui-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div
      class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden"
      [ngClass]="extraClasses"
    >
      <!-- Header -->
      @if (title || subtitle) {
        <div
          class="px-6 py-4 border-b border-gray-100 flex justify-between items-center bg-gray-50/50"
        >
          <div>
            <h3 class="text-lg font-semibold text-gray-800">{{ title }}</h3>
            @if (subtitle) {
              <p class="text-sm text-gray-500 mt-1">{{ subtitle }}</p>
            }
          </div>
        </div>
      }

      <!-- Body -->
      <div [ngClass]="noPadding ? '' : 'p-6'">
        <ng-content></ng-content>
      </div>

    </div>
  `,
})
export class UiCardComponent {
  @Input() title?: string;
  @Input() subtitle?: string;
  @Input() noPadding: boolean = false;
  @Input() extraClasses: string = '';
}
