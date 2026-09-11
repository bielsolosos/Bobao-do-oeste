import { Component, input } from '@angular/core';

@Component({
  selector: 'app-ui-form-field',
  standalone: true,
  template: `
    <div>
      <label [for]="fieldId()" class="mb-1 block text-sm font-medium text-brand-950">
        {{ label() }}
        @if (required()) {
          <span class="text-red-600" aria-hidden="true">*</span>
          <span class="sr-only">(obrigatório)</span>
        }
      </label>

      <ng-content></ng-content>

      @if (error()) {
        <p [id]="fieldId() + '-error'" class="mt-1 text-xs font-medium text-red-600" role="alert">
          {{ error() }}
        </p>
      } @else if (hint()) {
        <p [id]="fieldId() + '-hint'" class="mt-1 text-xs text-brand-950/50">{{ hint() }}</p>
      }
    </div>
  `,
})
export class UiFormFieldComponent {
  readonly fieldId = input.required<string>();
  readonly label = input.required<string>();
  readonly hint = input<string>('');
  readonly error = input<string>('');
  readonly required = input(false);
}
