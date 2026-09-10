import { CanDeactivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { UiConfirmService } from '../../shared/components/ui-confirm/ui-confirm.service';

export interface HasUnsavedChanges {
  hasUnsavedChanges(): boolean;
}

export const unsavedChangesGuard: CanDeactivateFn<HasUnsavedChanges> = (component) => {
  if (!component.hasUnsavedChanges()) return true;

  return inject(UiConfirmService).confirm({
    title: 'Descartar alterações?',
    message: 'Você tem alterações não salvas. Se sair agora, elas serão perdidas.',
    confirmText: 'Sair sem salvar',
    cancelText: 'Continuar editando',
    isDestructive: true,
  });
};
