import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, Subject } from 'rxjs';
import { WebhookService } from '../../core/services/webhook.service';
import { WebhookEventSummaryResponse, WebhookStatus } from '../../core/models/webhook.model';
import { UiCardComponent } from '../../shared/components/ui-card/ui-card.component';
import { UiBadgeComponent } from '../../shared/components/ui-badge/ui-badge.component';
import { UiButtonComponent } from '../../shared/components/ui-button/ui-button.component';
import { UiToastService } from '../../shared/components/ui-toast/ui-toast.service';
import { UiPaginationComponent } from '../../shared/components/ui-pagination/ui-pagination.component';
import { UiStatePanelComponent } from '../../shared/components/ui-state-panel/ui-state-panel.component';
import { UiPageHeaderComponent } from '../../shared/components/ui-page-header/ui-page-header.component';

type ListState = 'loading' | 'error' | 'ready';

@Component({
  selector: 'app-events-list',
  standalone: true,
  imports: [
    CommonModule,
    UiCardComponent,
    UiBadgeComponent,
    UiButtonComponent,
    UiPaginationComponent,
    UiStatePanelComponent,
    UiPageHeaderComponent,
  ],
  templateUrl: './events-list.component.html',
})
export class EventsListComponent implements OnInit {
  private webhookService = inject(WebhookService);
  private toast = inject(UiToastService);
  private destroyRef = inject(DestroyRef);
  private keywordSearch = new Subject<void>();

  state = signal<ListState>('loading');
  events = signal<WebhookEventSummaryResponse[]>([]);
  statusFilter = signal<WebhookStatus | ''>('');
  keyword = signal('');

  page = signal(0);
  pageSize = signal(25);

  totalElements = signal(0);
  totalPages = signal(0);

  constructor() {
    this.keywordSearch
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.loadEvents());
  }

  ngOnInit() {
    this.loadEvents();
  }

  loadEvents() {
    this.state.set('loading');
    this.webhookService
      .getEvents(this.page(), this.pageSize(), {
        status: this.statusFilter() || undefined,
        q: this.keyword(),
      })
      .subscribe({
        next: (res) => {
          this.events.set(res.content ?? []);
          this.page.set(res.number ?? 0);
          this.totalElements.set(res.totalElements ?? 0);
          this.totalPages.set(res.totalPages ?? 0);
          this.state.set('ready');
        },
        error: () => this.state.set('error'),
      });
  }

  onStatus(event: Event) {
    this.statusFilter.set((event.target as HTMLSelectElement).value as WebhookStatus | '');
    this.page.set(0);
    this.loadEvents();
  }

  onKeyword(event: Event) {
    this.keyword.set((event.target as HTMLInputElement).value);
    this.page.set(0);
    this.keywordSearch.next();
  }

  onPageChange(page: number) {
    this.page.set(page);
    this.loadEvents();
  }

  onPageSizeChange(size: number) {
    this.pageSize.set(size);
    this.page.set(0);
    this.loadEvents();
  }

  async copy(value: string, label: string) {
    if (!value) return;
    try {
      await navigator.clipboard.writeText(value);
      this.toast.success('Copiado', `${label} copiado para a área de transferência.`);
    } catch {
      this.toast.error('Não foi possível copiar', 'Copie o valor manualmente.');
    }
  }

  getStatusVariant(status: WebhookStatus): 'success' | 'danger' | 'warning' | 'neutral' {
    switch (status) {
      case 'PROCESSED':
        return 'success';
      case 'FAILED':
        return 'danger';
      case 'RECEIVED':
      case 'PROCESSING':
      case 'PENDING':
        return 'warning';
      case 'DUPLICATE':
        return 'neutral';
      default:
        return 'neutral';
    }
  }

  statusLabel(status: WebhookStatus): string {
    switch (status) {
      case 'PROCESSED':
        return 'Processado';
      case 'FAILED':
        return 'Falhou';
      case 'RECEIVED':
        return 'Recebido';
      case 'PROCESSING':
        return 'Processando';
      case 'PENDING':
        return 'Pendente';
      case 'DUPLICATE':
        return 'Duplicado';
      default:
        return status;
    }
  }
}
