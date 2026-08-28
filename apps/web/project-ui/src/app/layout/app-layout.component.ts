import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <!-- MAIN WRAPPER (Flex-col para mobile, Flex-row para desktop) -->
    <div class="h-screen w-full bg-slate-50 flex flex-col md:flex-row overflow-hidden">
      
      <!-- SIDEBAR (Apenas visível em telas md ou maiores) -->
      <aside class="hidden md:flex w-64 bg-slate-900 text-white flex-col shadow-xl z-20 flex-shrink-0 h-full">
        <div class="h-16 flex items-center px-6 border-b border-slate-800">
          <div class="h-8 w-8 rounded-lg bg-gradient-to-tr from-blue-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-blue-500/20">
            <svg class="h-5 w-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
          <span class="ml-3 text-lg font-bold tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-blue-100 to-white">BI Engine</span>
        </div>

        <nav class="flex-1 py-6 px-4 space-y-1.5 overflow-y-auto custom-scrollbar">
          
          <!-- Home -->
          <a routerLink="/dashboard" routerLinkActive="bg-blue-600/10 text-blue-400 border-l-4 border-blue-500" [routerLinkActiveOptions]="{exact: true}" class="flex items-center px-3 py-2.5 text-sm font-medium rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-all group">
            <svg class="mr-3 h-5 w-5 flex-shrink-0 group-hover:text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
            </svg>
            Dashboard
          </a>

          <!-- Monitores / Buscas -->
          <a routerLink="/monitors" routerLinkActive="bg-blue-600/10 text-blue-400 border-l-4 border-blue-500" class="flex items-center px-3 py-2.5 text-sm font-medium rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-all group mt-2">
            <svg class="mr-3 h-5 w-5 flex-shrink-0 group-hover:text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            Monitores Inteligentes
          </a>

          <!-- Infraestrutura -->
          <div class="pt-6 pb-2">
            <p class="px-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">Sistema</p>
          </div>

          <a routerLink="/events" routerLinkActive="bg-blue-600/10 text-blue-400 border-l-4 border-blue-500" class="flex items-center px-3 py-2.5 text-sm font-medium rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-all group">
            <svg class="mr-3 h-5 w-5 flex-shrink-0 group-hover:text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
            Eventos & Webhooks
          </a>

          <a routerLink="/ai-logs" routerLinkActive="bg-blue-600/10 text-blue-400 border-l-4 border-blue-500" class="flex items-center px-3 py-2.5 text-sm font-medium rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-all group">
            <svg class="mr-3 h-5 w-5 flex-shrink-0 group-hover:text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
            Auditoria IA
          </a>

        </nav>
      </aside>

      <!-- ÁREA DE CONTEÚDO -->
      <div class="flex-1 flex flex-col min-w-0 min-h-0 relative h-full">
        
        <!-- TOPBAR UNIVERSAL (Mobile e Desktop) -->
        <header class="bg-white border-b border-gray-200 h-16 flex-shrink-0 px-4 md:px-6 flex items-center justify-between z-10 shadow-sm relative">
          
          <!-- MOBILE LOGO -->
          <div class="flex md:hidden items-center">
            <div class="h-8 w-8 rounded-lg bg-gradient-to-tr from-blue-500 to-indigo-600 flex items-center justify-center mr-2">
              <svg class="h-5 w-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <h2 class="text-lg font-bold text-gray-800 tracking-tight">BI Engine</h2>
          </div>

          <!-- DESKTOP TITLE -->
          <div class="hidden md:flex items-center">
             <span class="text-gray-500 text-sm font-medium">Dashboard Administrativo</span>
          </div>

          <!-- USER DROPDOWN (Direita) -->
          <div class="flex items-center">
            <div class="relative group cursor-pointer">
              <div class="flex items-center gap-2 lg:gap-3 px-2 py-1.5 rounded-lg hover:bg-gray-50 transition-colors">
                <div class="flex flex-col text-right hidden sm:flex">
                  <span class="text-sm font-medium text-gray-900 leading-tight">
                    {{ authService.currentUser()?.username || 'Administrador' }}
                  </span>
                  <span class="text-xs text-gray-500 font-mono">{{ getPrimaryRole() }}</span>
                </div>
                <div class="h-9 w-9 rounded-full bg-blue-100 flex items-center justify-center text-blue-700 font-bold border border-blue-200 shadow-sm">
                  {{ (authService.currentUser()?.username || 'A').charAt(0).toUpperCase() }}
                </div>
              </div>

              <!-- Menu flutuante -->
              <div class="absolute right-0 mt-1 w-48 bg-white rounded-md shadow-lg py-1 ring-1 ring-black ring-opacity-5 hidden group-hover:block z-50">
                <div class="px-4 py-2 border-b border-gray-100 sm:hidden">
                  <p class="text-sm font-medium text-gray-900 truncate">{{ authService.currentUser()?.username }}</p>
                  <p class="text-xs text-gray-500 truncate">{{ getPrimaryRole() }}</p>
                </div>
                <button (click)="logout()" class="block w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition-colors">
                  Sair do Sistema
                </button>
              </div>
            </div>
          </div>
        </header>

        <!-- MAIN SCROLLABLE CONTENT (Padding bottom extra apenas no mobile para não sobrepor a Bottom Nav) -->
        <main class="flex-1 overflow-y-auto bg-slate-50 p-4 pb-24 md:pb-8 md:p-6 lg:p-8 relative">
           <router-outlet></router-outlet>
        </main>
      </div>

      <!-- BOTTOM NAVIGATION BAR (Apenas visível em telas mobile) -->
      <nav class="md:hidden fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 flex items-center justify-between z-50 h-[68px] px-1 shadow-[0_-2px_10px_rgba(0,0,0,0.05)]">
        
        <!-- Dashboard -->
        <a routerLink="/dashboard" routerLinkActive="text-blue-600" [routerLinkActiveOptions]="{exact: true}" class="flex-1 flex flex-col items-center justify-center h-full text-gray-500 hover:text-blue-600 transition-colors">
          <svg class="h-6 w-6 mb-1" fill="none" viewBox="0 0 24 24" stroke="currentColor" [class.text-blue-600]="isRouteActive('/dashboard')">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
          <span class="text-[10px] font-medium" [class.text-blue-600]="isRouteActive('/dashboard')" [class.font-bold]="isRouteActive('/dashboard')">Painel</span>
        </a>

        <!-- Monitores -->
        <a routerLink="/monitors" routerLinkActive="text-blue-600" class="flex-1 flex flex-col items-center justify-center h-full text-gray-500 hover:text-blue-600 transition-colors">
          <svg class="h-6 w-6 mb-1" fill="none" viewBox="0 0 24 24" stroke="currentColor" [class.text-blue-600]="isRouteActive('/monitors')">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <span class="text-[10px] font-medium" [class.text-blue-600]="isRouteActive('/monitors')" [class.font-bold]="isRouteActive('/monitors')">Buscas</span>
        </a>

        <!-- Eventos -->
        <a routerLink="/events" routerLinkActive="text-blue-600" class="flex-1 flex flex-col items-center justify-center h-full text-gray-500 hover:text-blue-600 transition-colors">
          <svg class="h-6 w-6 mb-1" fill="none" viewBox="0 0 24 24" stroke="currentColor" [class.text-blue-600]="isRouteActive('/events')">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
          <span class="text-[10px] font-medium" [class.text-blue-600]="isRouteActive('/events')" [class.font-bold]="isRouteActive('/events')">Eventos</span>
        </a>

        <!-- IA Logs -->
        <a routerLink="/ai-logs" routerLinkActive="text-blue-600" class="flex-1 flex flex-col items-center justify-center h-full text-gray-500 hover:text-blue-600 transition-colors">
          <svg class="h-6 w-6 mb-1" fill="none" viewBox="0 0 24 24" stroke="currentColor" [class.text-blue-600]="isRouteActive('/ai-logs')">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
          </svg>
          <span class="text-[10px] font-medium" [class.text-blue-600]="isRouteActive('/ai-logs')" [class.font-bold]="isRouteActive('/ai-logs')">IA</span>
        </a>

      </nav>

    </div>
  `,
  styles: [`
    .custom-scrollbar::-webkit-scrollbar {
      width: 4px;
    }
    .custom-scrollbar::-webkit-scrollbar-track {
      background: transparent;
    }
    .custom-scrollbar::-webkit-scrollbar-thumb {
      background-color: #334155;
      border-radius: 20px;
    }
  `]
})
export class AppLayoutComponent {
  authService = inject(AuthService);

  logout() {
    this.authService.logout();
  }

  isRouteActive(routePath: string): boolean {
    return window.location.pathname.startsWith(routePath);
  }

  getPrimaryRole(): string {
    const user = this.authService.currentUser();
    if (user && user.roles && user.roles.length > 0) {
      return user.roles[0].replace('ROLE_', '');
    }
    return 'SYSTEM';
  }
}
