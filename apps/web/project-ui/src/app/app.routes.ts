import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { LoginComponent } from './features/auth/login/login.component';
import { AppLayoutComponent } from './layout/app-layout.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { 
    path: '', 
    component: AppLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard-home.component').then(m => m.DashboardHomeComponent)
      },
      {
        path: 'monitors',
        loadComponent: () => import('./features/monitors/monitor-list/monitor-list.component').then(m => m.MonitorListComponent)
      },
      {
        path: 'monitors/new',
        loadComponent: () => import('./features/monitors/monitor-form/monitor-form.component').then(m => m.MonitorFormComponent)
      },
      {
        path: 'monitors/edit/:id',
        loadComponent: () => import('./features/monitors/monitor-form/monitor-form.component').then(m => m.MonitorFormComponent)
      },
      {
        path: 'monitors/:id',
        loadComponent: () => import('./features/monitors/monitor-detail/monitor-detail.component').then(m => m.MonitorDetailComponent)
      },
      {
        path: 'events',
        loadComponent: () => import('./features/events/events-list.component').then(m => m.EventsListComponent)
      },
      {
        path: 'ai-logs',
        loadComponent: () => import('./features/ai-logs/ai-logs-list.component').then(m => m.AiLogsListComponent)
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: '' }
];
