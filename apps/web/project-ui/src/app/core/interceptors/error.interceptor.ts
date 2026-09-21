import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { UiToastService } from '../../shared/components/ui-toast/ui-toast.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(UiToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Ignora 401 e 403, pois já são tratados pelo auth.interceptor.ts
      if (error.status !== 401 && error.status !== 403) {
        let title = 'Erro no Servidor';
        let message = 'Ocorreu um erro inesperado. Tente novamente mais tarde.';

        if (error.status >= 400 && error.status < 500) {
          title = 'Erro de Requisição';
          message =
            error.error?.message || 'Os dados enviados são inválidos ou a ação não é permitida.';
        } else if (error.status >= 500) {
          title = 'Erro Interno (500)';
          message = 'O servidor encontrou um erro ao processar sua requisição.';
        } else if (error.status === 0) {
          title = 'Sem Conexão';
          message = 'Não foi possível conectar ao servidor. Verifique sua internet.';
        }

        toast.error(title, message);
      }

      return throwError(() => error);
    }),
  );
};
