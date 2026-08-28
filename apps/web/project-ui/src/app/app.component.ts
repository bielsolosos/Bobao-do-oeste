import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { UiToastComponent } from './shared/components/ui-toast/ui-toast.component';
import { UiConfirmComponent } from './shared/components/ui-confirm/ui-confirm.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, UiToastComponent, UiConfirmComponent],
  template: `
    <router-outlet></router-outlet>
    <app-ui-toast-container></app-ui-toast-container>
    <app-ui-confirm-dialog></app-ui-confirm-dialog>
  `
})
export class AppComponent {
}
