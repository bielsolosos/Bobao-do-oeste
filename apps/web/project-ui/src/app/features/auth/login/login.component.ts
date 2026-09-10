import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { UiButtonComponent } from '../../../shared/components/ui-button/ui-button.component';
import { UiToastService } from '../../../shared/components/ui-toast/ui-toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, UiButtonComponent],
  template: `
    <div class="min-h-screen bg-gray-100 flex items-center justify-center p-4 relative z-10">
      <div class="max-w-md w-full bg-white rounded-xl shadow-lg p-8 relative z-20">
        <div class="mb-8 text-center">
          <div class="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-500 to-indigo-600 text-white shadow-lg shadow-brand-500/20">
            <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M13 10V3L4 14h7v7l9-11h-7Z" />
            </svg>
          </div>
          <h2 class="text-2xl font-bold tracking-tight text-slate-900">Bobão do Oeste</h2>
          <p class="mt-1 text-sm text-slate-500">Inteligência para suas buscas de produtos</p>
        </div>

        <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" class="space-y-6">
          <!-- MENSAGEM DE ERRO ESTATICA COMO BACKUP SEGURO -->
          @if (errorMessage()) {
            <div
              class="bg-red-50 text-red-600 p-3 rounded-lg text-sm text-center border border-red-100 font-medium animate-pulse"
            >
              {{ errorMessage() }}
            </div>
          }

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Usuário</label>
            <input
              id="username"
              type="text"
              autocomplete="username"
              aria-label="Usuário"
              formControlName="username"
              class="ui-input"
              placeholder="Ex: admin"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Senha</label>
            <input
              id="password"
              type="password"
              autocomplete="current-password"
              aria-label="Senha"
              formControlName="password"
              class="ui-input"
              placeholder="••••••••"
            />
          </div>

          <app-ui-button
            type="submit"
            [fullWidth]="true"
            size="lg"
            [loading]="isLoading()"
            [disabled]="loginForm.invalid || isLoading()"
          >
            Entrar
          </app-ui-button>
        </form>
      </div>
    </div>
  `,
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private toast = inject(UiToastService);

  loginForm = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  onSubmit() {
    if (this.loginForm.valid) {
      this.isLoading.set(true);
      this.errorMessage.set(null);

      this.authService.login({
        username: this.loginForm.value.username ?? '',
        password: this.loginForm.value.password ?? '',
      }).subscribe({
        next: () => {
          this.isLoading.set(false);
          this.toast.success('Sucesso', 'Bem-vindo de volta!');
        },
        error: (err) => {
          console.error('Login error capturado:', err);
          this.isLoading.set(false);

          if (err.status === 401 || err.status === 403) {
            this.errorMessage.set('Usuário ou senha incorretos.');
          } else {
            this.errorMessage.set('Falha ao conectar com o servidor.');
          }
        },
      });
    }
  }
}
