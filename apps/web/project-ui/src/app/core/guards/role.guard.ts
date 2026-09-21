import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Restringe rotas administrativas (Eventos e Auditoria de IA) a ROLE_ADMIN.
 * Aguarda o carregamento de /me quando há token, para não negar acesso indevidamente.
 */
export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  const user = authService.currentUser();
  const source$ = user ? of(user) : authService.loadMe();

  return source$.pipe(
    map((loaded) =>
      loaded?.roles?.includes('ROLE_ADMIN') ? true : router.createUrlTree(['/dashboard']),
    ),
    catchError(() => of(router.createUrlTree(['/dashboard']))),
  );
};

/** Impede que um usuário autenticado volte para a tela de login. */
export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.isAuthenticated() ? router.createUrlTree(['/dashboard']) : true;
};
