import {
  HttpErrorResponse,
  HttpClient,
  HttpContextToken,
  HttpInterceptorFn,
} from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import {
  catchError,
  finalize,
  map,
  Observable,
  shareReplay,
  switchMap,
  throwError,
} from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RefreshResponse {
  token: string;
  refreshToken?: string;
}

/**
 * Token de contexto para identificar requisições que já foram reprocessadas após o refresh do token,
 * evitando loop infinito caso o token renovado também retorne erro de autenticação.
 */
export const IS_RETRIED_REQUEST = new HttpContextToken<boolean>(() => false);

@Injectable({ providedIn: 'root' })
export class AuthSession {
  private router = inject(Router);
  private http = inject(HttpClient);

  // Armazena o Observable do refresh em andamento para criar uma fila única de requisições concorrentes
  private refreshInFlight$: Observable<string> | null = null;

  get isRefreshingNow(): boolean {
    return this.refreshInFlight$ !== null;
  }

  clearLocal(): void {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('refresh_token');
  }

  goToLogin(): void {
    void this.router.navigate(['/login'], { replaceUrl: true });
  }

  forceLogout(): void {
    this.clearLocal();
    this.goToLogin();
  }

  /**
   * Executa a renovação do token.
   * Se múltiplas requisições chamarem este método concorrentemente enquanto a renovação estiver em curso,
   * todas compartilharão o mesmo Observable (fila de espera) e serão reprocessadas quando o token chegar.
   */
  refreshToken(): Observable<string> {
    if (this.refreshInFlight$) {
      return this.refreshInFlight$;
    }

    const refreshToken = localStorage.getItem('refresh_token');
    if (!refreshToken) {
      this.forceLogout();
      return throwError(() => new Error('Refresh token não encontrado'));
    }

    this.refreshInFlight$ = this.http
      .post<RefreshResponse>(`${environment.apiUrl}/auth/refresh`, { refreshToken })
      .pipe(
        map((res) => {
          if (!res?.token) {
            throw new Error('Resposta de renovação não contém token');
          }
          localStorage.setItem('jwt_token', res.token);
          if (res.refreshToken) {
            localStorage.setItem('refresh_token', res.refreshToken);
          }
          return res.token;
        }),
        catchError((err) => {
          this.forceLogout();
          return throwError(() => err);
        }),
        finalize(() => {
          this.refreshInFlight$ = null;
        }),
        shareReplay(1),
      );

    return this.refreshInFlight$;
  }
}

const isAuthEndpoint = (url: string): boolean =>
  url.includes('/auth/login') || url.includes('/auth/refresh');

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const session = inject(AuthSession);

  // Não intercepta nem anexa tokens nas rotas públicas de autenticação
  if (isAuthEndpoint(req.url)) {
    return next(req);
  }

  const token = localStorage.getItem('jwt_token');
  let authReq = req;
  if (token && !req.headers.has('Authorization')) {
    authReq = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 || error.status === 403) {
        // Se a requisição já foi retentada anteriormente e falhou novamente, desloga para evitar loop
        if (authReq.context.get(IS_RETRIED_REQUEST)) {
          session.forceLogout();
          return throwError(() => error);
        }

        const refreshToken = localStorage.getItem('refresh_token');
        if (!refreshToken) {
          session.forceLogout();
          return throwError(() => error);
        }

        const currentToken = localStorage.getItem('jwt_token');
        const reqAuthHeader = authReq.headers.get('Authorization');
        const reqToken = reqAuthHeader?.replace(/^Bearer\s+/i, '');

        // Se o token no storage já for diferente do que a requisição utilizou,
        // significa que outra requisição acabou de renovar o token enquanto esta falhava.
        // Podemos reenviá-la imediatamente com o novo token sem disparar outro refresh.
        if (currentToken && reqToken && currentToken !== reqToken) {
          const retryReq = authReq.clone({
            setHeaders: { Authorization: `Bearer ${currentToken}` },
            context: authReq.context.set(IS_RETRIED_REQUEST, true),
          });
          return next(retryReq);
        }

        // Enfileira a requisição para aguardar a renovação e depois reprocessar a chamada original
        return session.refreshToken().pipe(
          switchMap((newToken) => {
            const retryReq = authReq.clone({
              setHeaders: { Authorization: `Bearer ${newToken}` },
              context: authReq.context.set(IS_RETRIED_REQUEST, true),
            });
            return next(retryReq);
          }),
        );
      }

      return throwError(() => error);
    }),
  );
};
