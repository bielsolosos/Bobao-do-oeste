import { Component, inject } from '@angular/core';

import { UiConfirmService } from './ui-confirm.service';
import { UiButtonComponent } from '../ui-button/ui-button.component';
import { UiDialogComponent } from '../ui-dialog/ui-dialog.component';

@Component({
  selector: 'app-ui-confirm-dialog',
  standalone: true,
  imports: [UiButtonComponent, UiDialogComponent],
  template: `
    <app-ui-dialog
      [open]="confirmService.isOpen()"
      [title]="confirmService.config()?.title || 'Confirmar ação'"
      [ariaLabel]="confirmService.config()?.title || 'Confirmar ação'"
      panelClass="sm:max-w-lg"
      (closed)="cancel()"
    >
      <div class="flex items-start gap-4">
        <div
          class="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full"
          [class]="
            confirmService.config()?.isDestructive
              ? 'bg-red-100 text-red-600'
              : 'bg-brand-amber/15 text-brand-amber-strong'
          "
          aria-hidden="true"
        >
          @if (confirmService.config()?.isDestructive) {
            <svg
              class="h-5 w-5"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
              />
            </svg>
          } @else {
            <svg
              class="h-5 w-5"
              fill="none"
              viewBox="0 0 24 24"
              stroke-width="1.5"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                d="M9.879 7.519c1.171-1.025 3.071-1.025 4.242 0 1.172 1.025 1.172 2.687 0 3.712-.203.179-.43.326-.67.442-.745.361-1.45.999-1.45 1.827v.75M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9 5.25h.008v.008H12v-.008z"
              />
            </svg>
          }
        </div>
        <p class="pt-1.5 text-sm leading-relaxed text-brand-950/70">
          {{ confirmService.config()?.message }}
        </p>
      </div>

      <div class="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <app-ui-button variant="outline" (clicked)="cancel()">
          {{ confirmService.config()?.cancelText }}
        </app-ui-button>
        <app-ui-button
          [variant]="confirmService.config()?.isDestructive ? 'danger' : 'primary'"
          (clicked)="confirm()"
        >
          {{ confirmService.config()?.confirmText }}
        </app-ui-button>
      </div>
    </app-ui-dialog>
  `,
})
export class UiConfirmComponent {
  confirmService = inject(UiConfirmService);

  confirm() {
    this.confirmService.close(true);
  }

  cancel() {
    this.confirmService.close(false);
  }
}
