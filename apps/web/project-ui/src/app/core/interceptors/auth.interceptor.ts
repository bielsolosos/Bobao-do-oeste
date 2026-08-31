import { HttpInterceptorFn, HttpErrorResponse, HttpClient, HttpRequest, HttpHandlerFn, HttpEvent } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

let isRefreshing = false;
let refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const http = inject(HttpClient);
  
  const token = localStorage.getItem('jwt_token');

  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/auth/login') && !req.url.includes('/auth/refresh')) {
        return handle401Error(req, next, http, router);
      }

      if (error.status === 401 && req.url.includes('/auth/refresh')) {
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('refresh_token');
        router.navigate(['/login']);
      }

      return throwError(() => error);
    })
  );
};

const handle401Error = (req: HttpRequest<any>, next: HttpHandlerFn, http: HttpClient, router: Router): Observable<HttpEvent<any>> => {
  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    const refreshToken = localStorage.getItem('refresh_token');

    if (refreshToken) {
      return http.post<any>(`${environment.apiUrl}/auth/refresh`, { refreshToken }).pipe(
        switchMap((res: any) => {
          isRefreshing = false;
          
          localStorage.setItem('jwt_token', res.token);
          if (res.refreshToken) {
             localStorage.setItem('refresh_token', res.refreshToken);
          }

          refreshTokenSubject.next(res.token);
          
          return next(req.clone({
            setHeaders: {
              Authorization: `Bearer ${res.token}`
            }
          }));
        }),
        catchError((err) => {
          isRefreshing = false;
          localStorage.removeItem('jwt_token');
          localStorage.removeItem('refresh_token');
          router.navigate(['/login']);
          return throwError(() => err);
        })
      );
    } else {
      isRefreshing = false;
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('refresh_token');
      router.navigate(['/login']);
      return throwError(() => new Error('No refresh token'));
    }
  } else {
    // wait for the new token from the ongoing refresh
    return refreshTokenSubject.pipe(
      filter(token => token !== null),
      take(1),
      switchMap(jwt => {
        return next(req.clone({
          setHeaders: {
            Authorization: `Bearer ${jwt}`
          }
        }));
      })
    );
  }
};
