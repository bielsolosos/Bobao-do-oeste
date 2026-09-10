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
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  isAuthenticated = signal<boolean>(this.hasToken());
  currentUser = signal<UserResponse | null>(null);

  constructor() {
    // Apenas se tiver token na subida a gente carrega o usuário.
    if (this.hasToken()) {
      this.loadMe().subscribe();
    }
  }

  login(credentials: LoginCredentials): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/login`, credentials).pipe(
      tap(response => {
        if (response.token) {
          localStorage.setItem('jwt_token', response.token);
          if (response.refreshToken) {
            localStorage.setItem('refresh_token', response.refreshToken);
          }
          this.isAuthenticated.set(true);
          
          this.loadMe().subscribe(() => {
            this.router.navigate(['/']);
          });
        }
      })
    );
  }

  loadMe(): Observable<UserResponse | null> {
    return this.http.get<UserResponse>(`${environment.apiUrl}/me`).pipe(
      tap(user => {
        this.currentUser.set(user);
      }),
      catchError(err => {
        console.error('Erro ao carregar usuário (F5 / me):', err);
        // ATENCAO: REMOVI O LOGOUT DAQUI! Se o interceptor detectar 401, ELE vai fazer o logout sozinho.
        // Fazer aqui também criava um comportamento onde qualquer falha de rede deslogava a pessoa ao dar F5.
        return of(null);
      })
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
}
