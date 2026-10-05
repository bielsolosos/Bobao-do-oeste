import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { InviteService } from '../../../core/services/invite.service';
import { UiButtonComponent } from '../../../shared/components/ui-button/ui-button.component';
import { UiToastService } from '../../../shared/components/ui-toast/ui-toast.service';

type PageState = 'loading' | 'valid' | 'invalid';

function passwordsMatchValidator(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;
  return password && confirmPassword && password !== confirmPassword
    ? { passwordsMismatch: true }
    : null;
}

@Component({
  selector: 'app-accept-invite',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, UiButtonComponent],
  templateUrl: './accept-invite.component.html',
})
export class AcceptInviteComponent implements OnInit {
  private fb = inject(FormBuilder);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private inviteService = inject(InviteService);
  private toast = inject(UiToastService);

  state = signal<PageState>('loading');
  inviteEmail = signal('');
  inviteToken = signal('');
  errorMessage = signal<string | null>(null);
  isSubmitting = signal(false);
  showPassword = signal(false);

  form = this.fb.group(
    {
      username: [
        '',
        [
          Validators.required,
          Validators.minLength(3),
          Validators.maxLength(50),
          Validators.pattern(/^[a-zA-Z0-9._-]+$/),
        ],
      ],
      password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(100)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: passwordsMatchValidator },
  );

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.state.set('invalid');
      this.errorMessage.set('Token do convite não fornecido na URL.');
      return;
    }

    this.inviteToken.set(token);
    this.inviteService.validateInvite(token).subscribe({
      next: (res) => {
        this.inviteEmail.set(res.email);
        this.state.set('valid');
      },
      error: (err) => {
        this.state.set('invalid');
        this.errorMessage.set(
          err.error?.message || 'Este convite é inválido, expirou ou já foi utilizado.',
        );
      },
    });
  }

  togglePassword(): void {
    this.showPassword.update((prev) => !prev);
  }

  isInvalid(controlName: string): boolean {
    const control = this.form.get(controlName);
    return !!control && control.invalid && (control.dirty || control.touched);
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  onSubmit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    const { username, password } = this.form.value;

    this.inviteService
      .acceptInvite(
        {
          token: this.inviteToken(),
          username: username!.trim(),
          password: password!,
        },
        '/dashboard',
      )
      .subscribe({
        next: () => {
          this.toast.success(
            'Conta criada com sucesso!',
            'Bem-vindo ao Bobão do Oeste. Boas caçadas!',
          );
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.errorMessage.set(
            err.error?.message || 'Falha ao concluir o cadastro. Verifique os dados.',
          );
        },
      });
  }
}
