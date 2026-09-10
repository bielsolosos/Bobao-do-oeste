import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-ui-form-field',
  standalone: true,
  template: `
    <div class="space-y-1.5">
      @if (label) {
        <label [for]="forId" class="block text-sm font-medium text-slate-700">
          {{ label }}
          @if (required) {
            <span class="text-red-500" aria-hidden="true">*</span>
          }
        </label>
      }

      <ng-content></ng-content>

      @if (error) {
        <p class="text-xs font-medium text-red-600" role="alert">{{ error }}</p>
      } @else if (hint) {
        <p class="text-xs text-slate-500">{{ hint }}</p>
      }
    </div>
  `,
})
export class UiFormFieldComponent {
  @Input() label = '';
  @Input() forId = '';
  @Input() hint = '';
  @Input() error = '';
  @Input() required = false;
}
