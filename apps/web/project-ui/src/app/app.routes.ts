import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { LoginComponent } from './features/auth/login/login.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { 
    path: '', 
    component: DashboardComponent,
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
        path: 'events',
        loadComponent: () => import('./features/events/events-list.component').then(m => m.EventsListComponent)
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: '' }
];
