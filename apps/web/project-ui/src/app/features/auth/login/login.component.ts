import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UiButtonComponent } from '../../../shared/components/ui-button/ui-button.component';
import { UiToastService } from '../../../shared/components/ui-toast/ui-toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, UiButtonComponent],
  template: `
    <div class="grid min-h-[100dvh] lg:grid-cols-2">
      <aside class="relative hidden overflow-hidden bg-brand-900 lg:block">
        <img
          src="assets/brand/login-scene.jpg"
          alt=""
          aria-hidden="true"
          class="h-full w-full object-cover"
        />
        <div class="absolute inset-0 bg-gradient-to-t from-brand-950 via-brand-950/40 to-transparent"></div>
        <div class="absolute bottom-0 left-0 right-0 p-10">
          <img
            src="assets/brand/marketplace-intelligence-mark.svg"
            alt=""
            class="mb-4 h-14 w-14"
            aria-hidden="true"
          />
          <h2 class="font-display text-3xl font-bold text-white">Bobão do Oeste</h2>
          <p class="mt-2 max-w-sm text-sm text-white/70">
            Um caçador inteligente de oportunidades em marketplaces. Rastreie, priorize e compre
            melhor.
          </p>
        </div>
      </aside>

      <main class="flex items-center justify-center bg-canvas p-6">
        <div class="w-full max-w-md">
          <div class="mb-8 flex flex-col items-center text-center lg:items-start lg:text-left">
            <img
              src="assets/brand/marketplace-intelligence-mark.svg"
              alt=""
              class="mb-4 h-14 w-14 lg:hidden"
              aria-hidden="true"
            />
            <p class="page-eyebrow">Área restrita</p>
            <h1 class="mt-1 font-display text-2xl font-bold text-brand-950">Entrar na plataforma</h1>
            <p class="mt-1 text-sm text-brand-950/60">
              Informe suas credenciais para continuar a caçada.
            </p>
          </div>

          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" class="space-y-5" novalidate>
            @if (errorMessage()) {
              <div
                class="rounded-lg border border-red-200 bg-red-50 p-3 text-sm font-medium text-red-700"
                role="alert"
              >
                {{ errorMessage() }}
              </div>
            }

            <div>
              <label for="login-username" class="mb-1 block text-sm font-medium text-brand-950">Usuário</label>
              <input
                id="login-username"
                type="text"
                formControlName="username"
                autocomplete="username"
                [attr.aria-invalid]="isInvalid('username')"
                [attr.aria-describedby]="isInvalid('username') ? 'login-username-error' : null"
                class="w-full rounded-lg border border-brand-950/20 bg-surface px-4 py-2.5 text-brand-950 transition-colors focus:border-brand-amber-strong focus:outline-none focus:ring-2 focus:ring-brand-amber-strong"
                placeholder="Seu nome de usuário"
              />
              @if (isInvalid('username')) {
                <p id="login-username-error" class="mt-1 text-xs font-medium text-red-600">
                  Informe o usuário.
                </p>
              }
            </div>

            <div>
              <label for="login-password" class="mb-1 block text-sm font-medium text-brand-950">Senha</label>
              <div class="relative">
                <input
                  id="login-password"
                  [type]="showPassword() ? 'text' : 'password'"
                  formControlName="password"
                  autocomplete="current-password"
                  [attr.aria-invalid]="isInvalid('password')"
                  [attr.aria-describedby]="isInvalid('password') ? 'login-password-error' : null"
                  class="w-full rounded-lg border border-brand-950/20 bg-surface px-4 py-2.5 pr-12 text-brand-950 transition-colors focus:border-brand-amber-strong focus:outline-none focus:ring-2 focus:ring-brand-amber-strong"
                  placeholder="Sua senha"
                />
                <button
                  type="button"
                  (click)="togglePassword()"
                  [attr.aria-pressed]="showPassword()"
                  class="absolute inset-y-0 right-0 flex items-center px-3 text-brand-950/50 hover:text-brand-950"
                >
                  <span class="sr-only">{{ showPassword() ? 'Ocultar senha' : 'Mostrar senha' }}</span>
                  <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                    @if (showPassword()) {
                      <path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
                    } @else {
                      <path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                      <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    }
                  </svg>
                </button>
              </div>
              @if (isInvalid('password')) {
                <p id="login-password-error" class="mt-1 text-xs font-medium text-red-600">
                  Informe a senha.
                </p>
              }
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

          <p class="mt-6 text-center text-xs text-brand-950/40 lg:text-left">
            Credenciais de desenvolvimento: <span class="font-mono">admin / admin123</span>
          </p>
        </div>
      </main>
    </div>
  `,
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private toast = inject(UiToastService);
  private route = inject(ActivatedRoute);

  loginForm = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  showPassword = signal(false);

  isInvalid(controlName: 'username' | 'password'): boolean {
    const control = this.loginForm.get(controlName);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  togglePassword() {
    this.showPassword.update((value) => !value);
  }

  onSubmit() {
    this.loginForm.markAllAsTouched();
    if (this.loginForm.invalid) return;

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/dashboard';

    this.authService.login(this.loginForm.value, returnUrl).subscribe({
      next: () => this.toast.success('Sucesso', 'Bem-vindo de volta!'),
      error: (err) => {
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
