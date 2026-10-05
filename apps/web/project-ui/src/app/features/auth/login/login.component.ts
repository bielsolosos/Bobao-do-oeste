import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UiButtonComponent } from '../../../shared/components/ui-button/ui-button.component';
import { UiToastService } from '../../../shared/components/ui-toast/ui-toast.service';
import { OtpLoginComponent } from './components/otp-login/otp-login.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, UiButtonComponent, OtpLoginComponent],
  templateUrl: './login.component.html',
})
export class LoginComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private toast = inject(UiToastService);
  private route = inject(ActivatedRoute);

  loginForm = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });

  emailOtpAvailable = signal(false);
  loginMode = signal<'password' | 'otp'>('password');
  isLoading = signal(false);
  errorMessage = signal<string | null>(null);
  showPassword = signal(false);

  ngOnInit(): void {
    this.authService.getAuthConfig().subscribe({
      next: (config) => {
        this.emailOtpAvailable.set(!!config?.emailOtpEnabled);
      },
      error: () => {
        this.emailOtpAvailable.set(false);
      },
    });
  }

  setLoginMode(mode: 'password' | 'otp') {
    this.loginMode.set(mode);
    this.errorMessage.set(null);
  }

  togglePassword() {
    this.showPassword.update((value) => !value);
  }

  isInvalid(controlName: 'username' | 'password'): boolean {
    const control = this.loginForm.get(controlName);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  onSubmitPassword() {
    this.loginForm.markAllAsTouched();
    if (this.loginForm.invalid) return;

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/dashboard';

    this.authService
      .login(
        {
          username: this.loginForm.value.username ?? '',
          password: this.loginForm.value.password ?? '',
        },
        returnUrl,
      )
      .subscribe({
        next: () => this.toast.success('Sucesso', 'Bem-vindo de volta!'),
        error: (err) => {
          this.isLoading.set(false);
          if (err.status === 401 || err.status === 403) {
            this.errorMessage.set('Usuário ou senha incorretos.');
          } else {
            this.errorMessage.set(err.error?.message || 'Falha ao conectar com o servidor.');
          }
        },
      });
  }
}
