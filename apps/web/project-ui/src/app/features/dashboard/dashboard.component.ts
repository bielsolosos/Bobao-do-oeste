import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="min-h-screen bg-gray-50 flex">
      <!-- Sidebar -->
      <aside class="w-64 bg-slate-900 text-slate-100 flex flex-col">
        <div class="p-4 bg-slate-950 font-bold text-xl flex items-center justify-center">
          🤖 BI Engine
        </div>
        <nav class="flex-1 p-4 space-y-2">
          <a routerLink="/dashboard" routerLinkActive="bg-blue-600 text-white" class="block px-4 py-2 rounded text-slate-300 hover:bg-slate-800 transition-colors">
            🏠 Visão Geral
          </a>
          <a routerLink="/monitors" routerLinkActive="bg-blue-600 text-white" class="block px-4 py-2 rounded text-slate-300 hover:bg-slate-800 transition-colors">
            🎯 Monitores
          </a>
          <a routerLink="/events" routerLinkActive="bg-blue-600 text-white" class="block px-4 py-2 rounded text-slate-300 hover:bg-slate-800 transition-colors ">
            📊 Eventos 
          </a>
        </nav>
        <div class="p-4 border-t border-slate-800">
          <button (click)="logout()" class="w-full px-4 py-2 text-sm bg-slate-800 hover:bg-red-600 rounded text-slate-300 hover:text-white transition-colors">
            Sair do Sistema
          </button>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="flex-1 overflow-y-auto bg-slate-50 p-8">
        <router-outlet></router-outlet>
      </main>
    </div>
  `
})
export class DashboardComponent {
  private authService = inject(AuthService);

  logout() {
    this.authService.logout();
  }
}
