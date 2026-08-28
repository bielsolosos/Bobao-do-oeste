import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { CommonModule } from '@angular/common';
import { UiButtonComponent } from '../../../shared/components/ui-button/ui-button.component';
import { UiToastService } from '../../../shared/components/ui-toast/ui-toast.service';
import { UiToastComponent } from '../../../shared/components/ui-toast/ui-toast.component';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, UiButtonComponent, UiToastComponent],
  template: `
    <!-- Renderizando container de Toast LOCALMENTE por garantia -->
    <app-ui-toast-container></app-ui-toast-container>

    <div class="min-h-screen bg-gray-100 flex items-center justify-center p-4 relative z-10">
      <div class="max-w-md w-full bg-white rounded-xl shadow-lg p-8 relative z-20">
        <h2 class="text-2xl font-bold text-center text-gray-800 mb-8">Login no BI Engine</h2>
        
        <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" class="space-y-6">

          <!-- MENSAGEM DE ERRO ESTATICA COMO BACKUP SEGURO -->
          <div *ngIf="errorMessage()" class="bg-red-50 text-red-600 p-3 rounded-lg text-sm text-center border border-red-100 font-medium animate-pulse">
            {{ errorMessage() }}
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Usuário</label>
            <input 
              type="text" 
              formControlName="username"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all bg-white"
              placeholder="Ex: admin"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Senha</label>
            <input 
              type="password" 
              formControlName="password"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition-all bg-white"
              placeholder="••••••••"
            />
          </div>

          <app-ui-button 
            type="submit" 
            [fullWidth]="true" 
            size="lg" 
            [loading]="isLoading()" 
            [disabled]="loginForm.invalid || isLoading()">
            Entrar
          </app-ui-button>
        </form>
      </div>
    </div>
  `
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private toast = inject(UiToastService);

  loginForm = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  onSubmit() {
    if (this.loginForm.valid) {
      this.isLoading.set(true);
      this.errorMessage.set(null);
      
      this.authService.login(this.loginForm.value).subscribe({
        next: (res) => {
          this.toast.success('Sucesso', 'Bem-vindo de volta!');
        },
        error: (err) => {
          console.error('Login error capturado:', err);
          this.isLoading.set(false);
          
          if (err.status === 401 || err.status === 403) {
            this.errorMessage.set('Usuário ou senha incorretos.');
            this.toast.error('Acesso Negado', 'Usuário ou senha incorretos.');
          } else {
            this.errorMessage.set('Falha ao conectar com o servidor.');
            this.toast.error('Erro', 'Falha ao conectar.');
          }
        }
      });
    }
  }
}
