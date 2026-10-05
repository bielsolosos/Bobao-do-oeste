import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { UiBadgeComponent } from '../../shared/components/ui-badge/ui-badge.component';
import { UiButtonComponent } from '../../shared/components/ui-button/ui-button.component';
import { UiCardComponent } from '../../shared/components/ui-card/ui-card.component';
import { UiFormFieldComponent } from '../../shared/components/ui-form-field/ui-form-field.component';
import { UiPageHeaderComponent } from '../../shared/components/ui-page-header/ui-page-header.component';
import { UiToastService } from '../../shared/components/ui-toast/ui-toast.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    UiPageHeaderComponent,
    UiCardComponent,
    UiFormFieldComponent,
    UiButtonComponent,
    UiBadgeComponent,
  ],
  templateUrl: './profile.component.html',
})
export class ProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  authService = inject(AuthService);
  private userService = inject(UserService);
  private toast = inject(UiToastService);

  isLoading = signal(true);
  isSavingCredentials = signal(false);
  isChangingPassword = signal(false);

  currentUser = computed(() => this.authService.currentUser());

  credentialsForm = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
  });

  passwordForm = this.fb.group({
    oldPassword: ['', [Validators.required]],
    oldPasswordConfirmation: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(6)]],
  });

  ngOnInit() {
    this.loadUserData();
  }

  loadUserData() {
    this.isLoading.set(true);
    this.authService.loadMe().subscribe({
      next: (user) => {
        if (user) {
          this.credentialsForm.patchValue({
            username: user.username,
            email: user.email || '',
          });
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erro ao carregar dados do usuário:', err);
        this.toast.error('Erro', 'Não foi possível carregar os dados do seu perfil.');
        this.isLoading.set(false);
      },
    });
  }

  onSaveCredentials() {
    this.credentialsForm.markAllAsTouched();
    if (this.credentialsForm.invalid) {
      this.toast.warning('Atenção', 'Verifique os dados informados no formulário de credenciais.');
      return;
    }

    const { username, email } = this.credentialsForm.getRawValue();
    if (!username || !email) return;

    this.isSavingCredentials.set(true);

    this.userService
      .editCredentials({
        username: username.trim(),
        email: email.trim(),
      })
      .subscribe({
        next: (updatedUser) => {
          this.isSavingCredentials.set(false);
          this.credentialsForm.markAsPristine();

          const current = this.authService.currentUser();
          if (current) {
            this.authService.currentUser.set({
              ...current,
              username: updatedUser.username,
              email: updatedUser.email,
            });
          }

          this.toast.success('Sucesso', 'Credenciais de acesso atualizadas com sucesso!');
        },
        error: (err) => {
          this.isSavingCredentials.set(false);
          console.error('Erro ao salvar credenciais:', err);
          this.toast.error(
            'Erro ao atualizar',
            err?.error?.message || 'Não foi possível atualizar suas credenciais.',
          );
        },
      });
  }

  onChangePassword() {
    this.passwordForm.markAllAsTouched();
    if (this.passwordForm.invalid) {
      this.toast.warning('Atenção', 'Preencha todos os campos da alteração de senha corretamente.');
      return;
    }

    const { oldPassword, oldPasswordConfirmation, newPassword } = this.passwordForm.getRawValue();

    if (oldPassword !== oldPasswordConfirmation) {
      this.toast.error('Atenção', 'A confirmação da senha atual não coincide com a senha digitada.');
      return;
    }

    const userId = this.currentUser()?.id;
    if (!userId) {
      this.toast.error('Erro', 'Identificador de usuário não encontrado.');
      return;
    }

    this.isChangingPassword.set(true);

    this.userService
      .changePassword(userId, {
        oldPassword: oldPassword!,
        oldPasswordConfirmation: oldPasswordConfirmation!,
        newPassword: newPassword!,
      })
      .subscribe({
        next: (response) => {
          this.isChangingPassword.set(false);
          this.passwordForm.reset();
          this.toast.success(
            'Senha Alterada',
            response.Message || response.message || 'Sua senha foi alterada com sucesso!',
          );
        },
        error: (err) => {
          this.isChangingPassword.set(false);
          console.error('Erro ao alterar senha:', err);
          this.toast.error(
            'Erro ao alterar senha',
            err?.error?.message ||
              err?.error?.Message ||
              'Não foi possível alterar sua senha. Verifique a senha atual.',
          );
        },
      });
  }

  primaryRoleLabel(): string {
    const roles = this.currentUser()?.roles ?? [];
    if (roles.includes('ROLE_ADMIN')) return 'Administrador';
    if (roles.length > 0) return roles[0].replace('ROLE_', '');
    return 'Usuário';
  }
}
