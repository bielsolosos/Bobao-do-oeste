import { CommonModule } from '@angular/common';
import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterModule } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs';
import { AuthService } from '../core/services/auth.service';

interface NavItem {
  label: string;
  shortLabel: string;
  path: string;
  icon: string;
  adminOnly?: boolean;
}

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="flex h-[100dvh] w-full flex-col overflow-hidden bg-canvas md:flex-row">
      <!-- Desktop Sidebar -->
      <aside
        class="z-[var(--z-nav)] hidden h-full flex-shrink-0 flex-col bg-brand-900 text-white shadow-xl transition-all duration-300 ease-in-out md:flex"
        [class.w-64]="!isSidebarCollapsed()"
        [class.w-20]="isSidebarCollapsed()"
        aria-label="Navegação lateral"
      >
        <!-- Brand Header -->
        <a
          routerLink="/dashboard"
          class="flex h-16 items-center border-b border-white/10 transition-colors hover:bg-white/5"
          [class.px-5]="!isSidebarCollapsed()"
          [class.justify-center]="isSidebarCollapsed()"
          [class.px-2]="isSidebarCollapsed()"
          [title]="isSidebarCollapsed() ? 'Bobão do Oeste - Monitor de Oportunidades' : ''"
        >
          <img
            src="assets/brand/marketplace-intelligence-mark.svg"
            alt=""
            class="h-9 w-9 flex-shrink-0"
            aria-hidden="true"
          />
          @if (!isSidebarCollapsed()) {
            <span class="min-w-0 transition-opacity duration-200">
              <span class="block truncate font-display text-base font-bold leading-tight text-white"
                >Bobão do Oeste</span
              >
              <span class="block truncate text-[11px] text-brand-brass"
                >Monitor de oportunidades</span
              >
            </span>
          }
        </a>

        <!-- Nav Items -->
        <nav
          class="flex-1 space-y-6 overflow-y-auto px-3 py-5"
          [class.px-2]="isSidebarCollapsed()"
        >
          <div>
            @if (!isSidebarCollapsed()) {
              <p
                class="px-3 pb-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-white/40"
              >
                Produto
              </p>
            } @else {
              <div class="my-2 border-t border-white/10"></div>
            }
            @for (item of productNav; track item.path) {
              <a
                [routerLink]="item.path"
                routerLinkActive="bg-white/10 text-white border-brand-amber"
                [routerLinkActiveOptions]="{ exact: item.path === '/dashboard' }"
                [title]="item.label"
                class="group mb-1 flex items-center rounded-lg border-l-2 border-transparent text-sm font-medium text-white/70 transition-colors hover:bg-white/5 hover:text-white"
                [class.px-3]="!isSidebarCollapsed()"
                [class.py-2.5]="!isSidebarCollapsed()"
                [class.justify-center]="isSidebarCollapsed()"
                [class.py-3]="isSidebarCollapsed()"
                [class.px-0]="isSidebarCollapsed()"
              >
                <span
                  class="h-5 w-5 flex-shrink-0"
                  [class.mr-3]="!isSidebarCollapsed()"
                  [innerHTML]="item.icon"
                  aria-hidden="true"
                ></span>
                @if (!isSidebarCollapsed()) {
                  <span class="truncate">{{ item.label }}</span>
                }
              </a>
            }
          </div>

          @if (isAdmin()) {
            <div>
              @if (!isSidebarCollapsed()) {
                <p
                  class="px-3 pb-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-white/40"
                >
                  Operação
                </p>
              } @else {
                <div class="my-2 border-t border-white/10"></div>
              }
              @for (item of operationNav; track item.path) {
                <a
                  [routerLink]="item.path"
                  routerLinkActive="bg-white/10 text-white border-brand-amber"
                  [title]="item.label"
                  class="group mb-1 flex items-center rounded-lg border-l-2 border-transparent text-sm font-medium text-white/70 transition-colors hover:bg-white/5 hover:text-white"
                  [class.px-3]="!isSidebarCollapsed()"
                  [class.py-2.5]="!isSidebarCollapsed()"
                  [class.justify-center]="isSidebarCollapsed()"
                  [class.py-3]="isSidebarCollapsed()"
                  [class.px-0]="isSidebarCollapsed()"
                >
                  <span
                    class="h-5 w-5 flex-shrink-0"
                    [class.mr-3]="!isSidebarCollapsed()"
                    [innerHTML]="item.icon"
                    aria-hidden="true"
                  ></span>
                  @if (!isSidebarCollapsed()) {
                    <span class="truncate">{{ item.label }}</span>
                  }
                </a>
              }
            </div>
          }
        </nav>

        <!-- Bottom Collapse Button -->
        <div class="border-t border-white/10 p-3">
          <button
            type="button"
            (click)="toggleSidebar()"
            [attr.aria-label]="isSidebarCollapsed() ? 'Expandir menu lateral' : 'Recolher menu lateral'"
            [title]="isSidebarCollapsed() ? 'Expandir menu lateral (Ctrl+B)' : 'Recolher menu lateral (Ctrl+B)'"
            class="flex w-full items-center rounded-lg py-2 text-xs font-medium text-white/60 transition-colors hover:bg-white/5 hover:text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-amber"
            [class.justify-center]="isSidebarCollapsed()"
            [class.px-3]="!isSidebarCollapsed()"
          >
            <svg
              class="h-5 w-5 flex-shrink-0 transition-transform duration-300"
              [class.rotate-180]="isSidebarCollapsed()"
              [class.mr-2.5]="!isSidebarCollapsed()"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              stroke-width="2"
            >
              <path stroke-linecap="round" stroke-linejoin="round" d="M11.25 4.5l-7.5 7.5 7.5 7.5m6-15l-7.5 7.5 7.5 7.5" />
            </svg>
            @if (!isSidebarCollapsed()) {
              <span>Recolher menu</span>
            }
          </button>
        </div>
      </aside>

      <!-- Main Workspace -->
      <div class="relative flex min-h-0 min-w-0 flex-1 flex-col">
        <header
          class="z-10 flex h-16 flex-shrink-0 items-center justify-between border-b border-brand-950/10 bg-surface px-4 shadow-sm md:px-6"
        >
          <div class="flex min-w-0 items-center gap-3">
            <!-- Header Sidebar Toggle Button -->
            <button
              type="button"
              (click)="toggleSidebar()"
              [attr.aria-label]="isSidebarCollapsed() ? 'Expandir menu lateral' : 'Recolher menu lateral'"
              [title]="isSidebarCollapsed() ? 'Expandir menu lateral (Ctrl+B)' : 'Recolher menu lateral (Ctrl+B)'"
              class="hidden h-9 w-9 items-center justify-center rounded-lg text-brand-950/60 transition-colors hover:bg-brand-950/5 hover:text-brand-950 focus-visible:outline focus-visible:outline-2 focus-visible:outline-brand-amber-strong md:inline-flex"
            >
              <svg
                class="h-5 w-5 transition-transform duration-300"
                [class.rotate-180]="isSidebarCollapsed()"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="2"
              >
                <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25H12" />
              </svg>
            </button>

            <img
              src="assets/brand/marketplace-intelligence-mark.svg"
              alt=""
              class="h-8 w-8 md:hidden"
              aria-hidden="true"
            />
            <div class="min-w-0">
              <p class="truncate font-display text-sm font-semibold text-brand-950 md:text-base">
                {{ pageTitle() }}
              </p>
              <p class="hidden truncate text-xs text-brand-950/50 sm:block">{{ pageSubtitle() }}</p>
            </div>
          </div>

          <div class="relative">
            <button
              type="button"
              (click)="toggleMenu($event)"
              [attr.aria-expanded]="menuOpen()"
              aria-haspopup="true"
              class="flex items-center gap-2 rounded-lg px-2 py-1.5 transition-colors hover:bg-brand-950/5 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2"
            >
              <span class="hidden flex-col text-right sm:flex">
                <span class="text-sm font-medium leading-tight text-brand-950">
                  {{ authService.currentUser()?.username || 'Usuário' }}
                </span>
                <span class="text-xs text-brand-950/50">{{ primaryRoleLabel() }}</span>
              </span>
              <span
                class="flex h-9 w-9 items-center justify-center rounded-full bg-brand-amber/20 font-bold text-brand-amber-strong"
              >
                {{ (authService.currentUser()?.username || 'A').charAt(0).toUpperCase() }}
              </span>
            </button>

            @if (menuOpen()) {
              <div
                class="absolute right-0 z-50 mt-2 w-56 overflow-hidden rounded-xl border border-brand-950/10 bg-surface py-1 shadow-[var(--shadow-pop)]"
                role="menu"
              >
                <div class="border-b border-brand-950/10 px-4 py-2">
                  <p class="truncate text-sm font-medium text-brand-950">
                    {{ authService.currentUser()?.username }}
                  </p>
                  <p class="truncate text-xs text-brand-950/50">
                    {{ authService.currentUser()?.email || primaryRoleLabel() }}
                  </p>
                </div>
                <button
                  type="button"
                  role="menuitem"
                  (click)="logout()"
                  class="block w-full px-4 py-2 text-left text-sm font-medium text-red-600 transition-colors hover:bg-red-50"
                >
                  Sair do sistema
                </button>
              </div>
            }
          </div>
        </header>

        <main class="flex-1 overflow-y-auto bg-canvas p-4 pb-28 md:p-6 md:pb-8 lg:p-8">
          <router-outlet></router-outlet>
        </main>
      </div>

      <!-- Mobile Bottom Navigation -->
      <nav
        class="safe-bottom fixed bottom-0 left-0 right-0 z-50 flex items-stretch border-t border-brand-950/10 bg-surface px-1 shadow-[0_-2px_10px_rgba(15,14,13,0.06)] md:hidden"
        aria-label="Navegação principal"
      >
        @for (item of mobileNav(); track item.path) {
          <a
            [routerLink]="item.path"
            routerLinkActive="text-brand-amber-strong"
            [routerLinkActiveOptions]="{ exact: item.path === '/dashboard' }"
            class="flex flex-1 flex-col items-center justify-center gap-0.5 py-2.5 text-brand-950/50 transition-colors"
          >
            <span class="h-6 w-6" [innerHTML]="item.icon" aria-hidden="true"></span>
            <span class="text-[11px] font-medium">{{ item.shortLabel }}</span>
          </a>
        }
      </nav>
    </div>
  `,
})
export class AppLayoutComponent {
  authService = inject(AuthService);
  private router = inject(Router);

  menuOpen = signal(false);
  isSidebarCollapsed = signal<boolean>(
    typeof window !== 'undefined' && localStorage.getItem('sidebar_collapsed') === 'true',
  );

  readonly productNav: NavItem[] = [
    {
      label: 'Visão geral',
      shortLabel: 'Visão',
      path: '/dashboard',
      icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"/></svg>',
    },
    {
      label: 'Monitores',
      shortLabel: 'Monitores',
      path: '/monitors',
      icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>',
    },
  ];

  readonly operationNav: NavItem[] = [
    {
      label: 'Eventos e webhooks',
      shortLabel: 'Eventos',
      path: '/events',
      adminOnly: true,
      icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z"/></svg>',
    },
    {
      label: 'Auditoria de IA',
      shortLabel: 'IA',
      path: '/ai-logs',
      adminOnly: true,
      icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/></svg>',
    },
  ];

  isAdmin = computed(() => this.authService.isAdmin());

  mobileNav = computed<NavItem[]>(() => {
    const nav: NavItem[] = [...this.productNav];
    if (this.isAdmin()) {
      nav.push(this.operationNav[0]);
      nav.push(this.operationNav[1]);
    }
    return nav;
  });

  private url = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects),
      startWith(this.router.url),
    ),
    { initialValue: this.router.url },
  );

  pageTitle = computed(() => {
    const url = this.url();
    if (url.startsWith('/monitors/new')) return 'Novo monitor';
    if (url.startsWith('/monitors/edit')) return 'Editar monitor';
    if (url.startsWith('/monitors/')) return 'Detalhe do monitor';
    if (url.startsWith('/monitors')) return 'Monitores';
    if (url.startsWith('/events')) return 'Eventos e webhooks';
    if (url.startsWith('/ai-logs')) return 'Auditoria de IA';
    return 'Visão geral';
  });

  pageSubtitle = computed(() => {
    if (this.pageTitle() === 'Visão geral') return 'Resumo da sua operação de caça a oportunidades';
    return 'Bobão do Oeste · Monitor inteligente de oportunidades';
  });

  primaryRoleLabel(): string {
    const roles = this.authService.currentUser()?.roles ?? [];
    if (roles.includes('ROLE_ADMIN')) return 'Administrador';
    if (roles.length > 0) return roles[0].replace('ROLE_', '');
    return 'Usuário';
  }

  toggleSidebar() {
    this.isSidebarCollapsed.update((collapsed) => {
      const next = !collapsed;
      if (typeof window !== 'undefined') {
        localStorage.setItem('sidebar_collapsed', String(next));
      }
      return next;
    });
  }

  @HostListener('document:keydown', ['$event'])
  onKeyDown(event: KeyboardEvent) {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'b') {
      event.preventDefault();
      this.toggleSidebar();
    }
  }

  toggleMenu(event: MouseEvent) {
    event.stopPropagation();
    this.menuOpen.update((open) => !open);
  }

  @HostListener('document:click')
  closeMenu() {
    this.menuOpen.set(false);
  }

  @HostListener('document:keydown.escape')
  onEscape() {
    this.menuOpen.set(false);
  }

  logout() {
    this.menuOpen.set(false);
    this.authService.logout();
  }
}
