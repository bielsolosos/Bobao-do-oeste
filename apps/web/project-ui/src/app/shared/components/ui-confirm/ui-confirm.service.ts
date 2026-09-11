import { Injectable, signal } from '@angular/core';

export interface ConfirmDialogConfig {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  isDestructive?: boolean;
}

@Injectable({ providedIn: 'root' })
export class UiConfirmService {
  readonly isOpen = signal<boolean>(false);
  readonly config = signal<ConfirmDialogConfig | null>(null);

  private resolver: ((value: boolean) => void) | null = null;

  confirm(config: ConfirmDialogConfig): Promise<boolean> {
    this.config.set({
      confirmText: 'Confirmar',
      cancelText: 'Cancelar',
      isDestructive: false,
      ...config,
    });
    this.isOpen.set(true);

    return new Promise<boolean>((resolve) => {
      this.resolver = resolve;
    });
  }

  close(result: boolean) {
    this.isOpen.set(false);
    this.resolver?.(result);
    this.resolver = null;
  }
}
