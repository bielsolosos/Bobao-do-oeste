import {
  HttpErrorResponse,
  HttpClient,
  HttpEvent,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, catchError, filter, Observable, switchMap, take, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

interface RefreshResponse {
  token: string;
  refreshToken?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthSession {
  private router = inject(Router);

  private isRefreshing = false;
  private readonly tokenSubject = new BehaviorSubject<string | null>(null);

  get token$(): Observable<string | null> {
    return this.tokenSubject.asObservable();
  }

  get isRefreshingNow(): boolean {
    return this.isRefreshing;
  }

  clearLocal(): void {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('refresh_token');
    this.tokenSubject.next(null);
  }

  goToLogin(): void {
    void this.router.navigate(['/login'], { replaceUrl: true });
  }

  forceLogout(): void {
    this.clearLocal();
    this.goToLogin();
  }

  startRefresh(
    http: HttpClient,
    onSuccess: (tokens: RefreshResponse) => void,
    onFailure: (err: unknown) => void,
  ): void {
    if (this.isRefreshing) {
      return;
    }
    this.isRefreshing = true;
    this.tokenSubject.next(null);

    const refreshToken = localStorage.getItem('refresh_token');
    if (!refreshToken) {
      this.isRefreshing = false;
      this.forceLogout();
      onFailure(new Error('No refresh token available'));
      return;
    }

    http.post<RefreshResponse>(`${environment.apiUrl}/auth/refresh`, { refreshToken }).subscribe({
      next: (res) => {
        if (res?.token) {
          localStorage.setItem('jwt_token', res.token);
          if (res.refreshToken) {
            localStorage.setItem('refresh_token', res.refreshToken);
          }
          this.tokenSubject.next(res.token);
          onSuccess(res);
        } else {
          this.forceLogout();
          onFailure(new Error('Refresh response missing token'));
        }
        this.isRefreshing = false;
      },
      error: (err) => {
        this.forceLogout();
        onFailure(err);
        this.isRefreshing = false;
      },
    });
  }
}

const isAuthEndpoint = (url: string): boolean =>
  url.includes('/auth/login') || url.includes('/auth/refresh');

const retryWithFreshToken = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  session: AuthSession,
): Observable<HttpEvent<unknown>> =>
  session.token$.pipe(
    filter((token): token is string => token !== null),
    take(1),
    switchMap((token) => next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }))),
  );

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const session = inject(AuthSession);
  const http = inject(HttpClient);

  const token = localStorage.getItem('jwt_token');
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (isAuthEndpoint(req.url)) {
        return throwError(() => error);
      }

      if (error.status === 401 || error.status === 403) {
        if (!localStorage.getItem('refresh_token')) {
          session.forceLogout();
          return throwError(() => error);
        }

        return new Observable<HttpEvent<unknown>>((subscriber) => {
          session.startRefresh(
            http,
            () => {
              retryWithFreshToken(req, next, session).subscribe({
                next: (event) => {
                  subscriber.next(event);
                  subscriber.complete();
                },
                error: (retryErr) => subscriber.error(retryErr),
              });
            },
            (refreshErr) => subscriber.error(refreshErr),
          );
        });
      }

      return throwError(() => error);
    }),
  );
};
