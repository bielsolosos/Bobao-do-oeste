import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { pendingChangesGuard } from './core/guards/pending-changes.guard';
import { adminGuard, guestGuard } from './core/guards/role.guard';
import { LoginComponent } from './features/auth/login/login.component';
import { NotFoundComponent } from './features/not-found/not-found.component';
import { AppLayoutComponent } from './layout/app-layout.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [guestGuard],
    title: 'Entrar · Bobão do Oeste',
  },
  //TODO aqui nesse path daqui também tem que passar o título da barra de cima do componente AppLayoutComponent.
  {
    path: '',
    component: AppLayoutComponent,
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard-home.component').then(
            (m) => m.DashboardHomeComponent,
          ),
        title: 'Visão geral · Bobão do Oeste',
      },
      {
        path: 'monitors',
        loadComponent: () =>
          import('./features/monitors/monitor-list/monitor-list.component').then(
            (m) => m.MonitorListComponent,
          ),
        title: 'Monitores · Bobão do Oeste',
      },
      {
        path: 'monitors/new',
        loadComponent: () =>
          import('./features/monitors/monitor-form/monitor-form.component').then(
            (m) => m.MonitorFormComponent,
          ),
        canDeactivate: [pendingChangesGuard],
        title: 'Novo monitor · Bobão do Oeste',
      },
      {
        path: 'monitors/edit/:id',
        loadComponent: () =>
          import('./features/monitors/monitor-form/monitor-form.component').then(
            (m) => m.MonitorFormComponent,
          ),
        canDeactivate: [pendingChangesGuard],
        title: 'Editar monitor · Bobão do Oeste',
      },
      {
        path: 'monitors/:id',
        loadComponent: () =>
          import('./features/monitors/monitor-detail/monitor-detail.component').then(
            (m) => m.MonitorDetailComponent,
          ),
        title: 'Detalhe do monitor · Bobão do Oeste',
      },
      {
        path: 'events',
        loadComponent: () =>
          import('./features/events/events-list.component').then((m) => m.EventsListComponent),
        canActivate: [adminGuard],
        title: 'Eventos · Bobão do Oeste',
      },
      {
        path: 'ai-logs',
        loadComponent: () =>
          import('./features/ai-logs/ai-logs-list.component').then((m) => m.AiLogsListComponent),
        canActivate: [adminGuard],
        title: 'Auditoria de IA · Bobão do Oeste',
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/settings.component').then((m) => m.SettingsComponent),
        title: 'Configurações de IA · Bobão do Oeste',
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile.component').then((m) => m.ProfileComponent),
        title: 'Perfil e Segurança · Bobão do Oeste',
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    ],
  },
  { path: '**', component: NotFoundComponent, title: 'Página não encontrada · Bobão do Oeste' },
];
