import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastMessage {
  id: string;
  type: ToastType;
  title: string;
  message?: string;
  duration?: number;
}

const MAX_VISIBLE_TOASTS = 4;

@Injectable({ providedIn: 'root' })
export class UiToastService {
  readonly toasts = signal<ToastMessage[]>([]);

  private counter = 0;

  show(type: ToastType, title: string, message?: string, duration: number = 4000) {
    const current = this.toasts();

    // Evita avalanches quando a mesma falha é reportada por interceptor e componente.
    const isDuplicate = current.some(
      (t) => t.type === type && t.title === title && t.message === message,
    );
    if (isDuplicate) return;

    const id = `toast-${++this.counter}`;
    const toast: ToastMessage = { id, type, title, message, duration };

    this.toasts.set([...current, toast].slice(-MAX_VISIBLE_TOASTS));

    if (duration > 0) {
      setTimeout(() => this.remove(id), duration);
    }
  }

  success(title: string, message?: string) {
    this.show('success', title, message);
  }
  error(title: string, message?: string) {
    this.show('error', title, message, 6000);
  }
  warning(title: string, message?: string) {
    this.show('warning', title, message, 5000);
  }
  info(title: string, message?: string) {
    this.show('info', title, message);
  }

  remove(id: string) {
    this.toasts.update((t) => t.filter((toast) => toast.id !== id));
  }
}
