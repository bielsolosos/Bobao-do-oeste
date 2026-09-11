import { CanDeactivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { UiConfirmService } from '../../shared/components/ui-confirm/ui-confirm.service';

export interface ComponentWithPendingChanges {
  hasUnsavedChanges(): boolean;
}

export const pendingChangesGuard: CanDeactivateFn<ComponentWithPendingChanges> = (component) => {
  if (!component?.hasUnsavedChanges?.()) {
    return true;
  }

  const confirmService = inject(UiConfirmService);
  return confirmService.confirm({
    title: 'Descartar alterações?',
    message: 'Você tem alterações não salvas. Se sair agora, elas serão perdidas.',
    confirmText: 'Descartar',
    cancelText: 'Continuar editando',
    isDestructive: true,
  });
};
