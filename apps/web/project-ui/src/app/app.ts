import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { UiConfirmComponent } from './shared/components/ui-confirm/ui-confirm.component';
import { UiToastComponent } from './shared/components/ui-toast/ui-toast.component';

@Component({
  imports: [RouterOutlet, UiToastComponent, UiConfirmComponent],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class AppComponent {
}
