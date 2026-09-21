import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { tap, catchError, of, Observable } from 'rxjs';
import { Router } from '@angular/router';
import { UserResponse } from '../models/user.model';

export interface LoginResponse {
  token: string;
  refreshToken?: string;
}

export interface LoginCredentials {
  username: string;
  password: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  isAuthenticated = signal<boolean>(this.hasToken());
  currentUser = signal<UserResponse | null>(null);

  constructor() {
    if (this.hasToken()) {
      this.loadMe().subscribe();
    }
  }

  login(credentials: LoginCredentials, redirectTo: string = '/'): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, credentials).pipe(
      tap((response) => {
        if (response.token) {
          localStorage.setItem('jwt_token', response.token);
          if (response.refreshToken) {
            localStorage.setItem('refresh_token', response.refreshToken);
          }
          this.isAuthenticated.set(true);

          this.loadMe().subscribe(() => {
            this.router.navigateByUrl(redirectTo);
          });
        }
      }),
    );
  }

  loadMe(): Observable<UserResponse | null> {
    return this.http.get<UserResponse>(`${environment.apiUrl}/me`).pipe(
      tap((user) => {
        this.currentUser.set(user);
      }),
      catchError((err) => {
        console.error('Erro ao carregar usuário (F5 / me):', err);
        // O interceptor trata 401/403. Não deslogamos aqui para não derrubar o usuário
        // em falhas transitórias de rede ao recarregar a página.
        return of(null);
      }),
    );
  }

  logout() {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('refresh_token');
    this.isAuthenticated.set(false);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem('jwt_token');
  }

  hasToken(): boolean {
    return !!localStorage.getItem('jwt_token');
  }

  hasRole(role: string): boolean {
    const user = this.currentUser();
    return !!user?.roles?.includes(role);
  }

  isAdmin(): boolean {
    return this.hasRole('ROLE_ADMIN');
  }
}
