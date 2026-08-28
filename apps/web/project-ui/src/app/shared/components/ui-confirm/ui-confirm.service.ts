import { Injectable, signal } from '@angular/core';
import { Subject } from 'rxjs';

export interface ConfirmDialogConfig {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  isDestructive?: boolean;
}

@Injectable({ providedIn: 'root' })
export class UiConfirmService {
  isOpen = signal<boolean>(false);
  config = signal<ConfirmDialogConfig | null>(null);
  
  private responseSubject: Subject<boolean> | null = null;

  confirm(config: ConfirmDialogConfig): Promise<boolean> {
    this.config.set({
      confirmText: 'Confirmar',
      cancelText: 'Cancelar',
      isDestructive: false,
      ...config
    });
    this.isOpen.set(true);

    this.responseSubject = new Subject<boolean>();
    return new Promise((resolve) => {
      this.responseSubject?.subscribe(res => {
        resolve(res);
      });
    });
  }

  close(result: boolean) {
    this.isOpen.set(false);
    if (this.responseSubject) {
      this.responseSubject.next(result);
      this.responseSubject.complete();
      this.responseSubject = null;
    }
  }
}
