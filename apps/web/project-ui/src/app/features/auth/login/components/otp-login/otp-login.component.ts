import { Component, EventEmitter, Input, OnDestroy, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../../../../core/services/auth.service';
import { UiButtonComponent } from '../../../../../shared/components/ui-button/ui-button.component';
import { UiToastService } from '../../../../../shared/components/ui-toast/ui-toast.service';

@Component({
  selector: 'app-otp-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, UiButtonComponent],
  templateUrl: './otp-login.component.html',
})
export class OtpLoginComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private toast = inject(UiToastService);
  private route = inject(ActivatedRoute);

  @Input() set initialIdentifier(val: string) {
    if (val && !this.requestForm.value.identifier) {
      this.requestForm.patchValue({ identifier: val });
    }
  }

  @Output() loginSuccess = new EventEmitter<void>();

  requestForm = this.fb.group({
    identifier: ['', Validators.required],
  });

  verifyForm = this.fb.group({
    code: ['', [Validators.required, Validators.pattern(/^[0-9]{6}$/)]],
  });

  otpStep = signal<'request' | 'verify'>('request');
  otpIdentifier = signal('');
  otpCooldown = signal(0);
  isSending = signal(false);
  isVerifying = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  private cooldownTimer: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {}

  ngOnDestroy(): void {
    this.stopCooldown();
  }

  isRequestInvalid(controlName: 'identifier'): boolean {
    const control = this.requestForm.get(controlName);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  isVerifyInvalid(controlName: 'code'): boolean {
    const control = this.verifyForm.get(controlName);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  onRequestOtp() {
    this.requestForm.markAllAsTouched();
    if (this.requestForm.invalid) return;

    const identifier = this.requestForm.value.identifier?.trim() || '';
    this.isSending.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.authService.sendOtp({ identifier }).subscribe({
      next: (res) => {
        this.isSending.set(false);
        this.otpIdentifier.set(identifier);
        this.otpStep.set('verify');
        this.verifyForm.reset();
        this.startCooldown(60);
        this.successMessage.set(res.message || 'Código enviado para seu e-mail.');
        this.toast.success('Código enviado', 'Verifique a caixa de entrada do e-mail cadastrado.');
      },
      error: (err) => {
        this.isSending.set(false);
        this.errorMessage.set(
          err.error?.message || 'Não foi possível enviar o código. Verifique os dados digitados.',
        );
      },
    });
  }

  onVerifyOtp() {
    this.verifyForm.markAllAsTouched();
    if (this.verifyForm.invalid) return;

    const identifier = this.otpIdentifier();
    const code = this.verifyForm.value.code?.trim() || '';
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/dashboard';

    this.isVerifying.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.authService.verifyOtp({ identifier, code }, returnUrl).subscribe({
      next: () => {
        this.toast.success('Sucesso', 'Autenticado com sucesso! Bem-vindo de volta.');
        this.loginSuccess.emit();
      },
      error: (err) => {
        this.isVerifying.set(false);
        this.errorMessage.set(
          err.error?.message || 'Código inválido ou expirado. Tente novamente.',
        );
      },
    });
  }

  onResendOtp() {
    if (this.otpCooldown() > 0 || this.isSending()) return;

    const identifier = this.otpIdentifier();
    if (!identifier) {
      this.onBackToRequest();
      return;
    }

    this.isSending.set(true);
    this.errorMessage.set(null);

    this.authService.sendOtp({ identifier }).subscribe({
      next: (res) => {
        this.isSending.set(false);
        this.startCooldown(60);
        this.successMessage.set(res.message || 'Novo código enviado.');
        this.toast.success('Código reenviado', 'Confira sua caixa de entrada.');
      },
      error: (err) => {
        this.isSending.set(false);
        this.errorMessage.set(err.error?.message || 'Falha ao reenviar código.');
      },
    });
  }

  onBackToRequest() {
    this.otpStep.set('request');
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  private startCooldown(seconds: number) {
    this.stopCooldown();
    this.otpCooldown.set(seconds);
    this.cooldownTimer = setInterval(() => {
      this.otpCooldown.update((prev) => {
        if (prev <= 1) {
          this.stopCooldown();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  }

  private stopCooldown() {
    if (this.cooldownTimer) {
      clearInterval(this.cooldownTimer);
      this.cooldownTimer = null;
    }
  }
}
