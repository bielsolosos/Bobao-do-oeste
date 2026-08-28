import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Evitar injeção de AuthService direto para não criar dependência circular (HttpClient -> Interceptor -> AuthService -> HttpClient)
  const router = inject(Router);
  
  // Pegamos o token nativamente para não quebrar a injeção circular
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
      // Se der 401 e não for a própria rota de login, limpa e desloga preventivamente.
      if (error.status === 401 && !req.url.includes('/auth/login')) {
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('refresh_token');
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
