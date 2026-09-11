import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
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

  state = signal<ListState>('loading');
  events = signal<WebhookEventSummaryResponse[]>([]);
  statusFilter = signal<WebhookStatus | ''>('');
  keyword = signal('');

  page = signal(0);
  pageSize = signal(25);

  filtered = computed(() => {
    const status = this.statusFilter();
    const kw = this.keyword().trim().toLowerCase();
    return this.events().filter((event) => {
      if (status && event.status !== status) return false;
      if (kw && !`${event.jobId} ${event.requestId} ${event.eventType}`.toLowerCase().includes(kw)) {
        return false;
      }
      return true;
    });
  });

  totalPages = computed(() => Math.max(1, Math.ceil(this.filtered().length / this.pageSize())));
  paged = computed(() => {
    const start = this.page() * this.pageSize();
    return this.filtered().slice(start, start + this.pageSize());
  });

  ngOnInit() {
    this.loadEvents();
  }

  loadEvents() {
    this.state.set('loading');
    this.webhookService.getEvents(0, 200).subscribe({
      next: (res) => {
        this.events.set(res.content ?? []);
        this.page.set(0);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }

  onStatus(event: Event) {
    this.statusFilter.set((event.target as HTMLSelectElement).value as WebhookStatus | '');
    this.page.set(0);
  }

  onKeyword(event: Event) {
    this.keyword.set((event.target as HTMLInputElement).value);
    this.page.set(0);
  }

  onPageChange(page: number) {
    this.page.set(page);
  }

  onPageSizeChange(size: number) {
    this.pageSize.set(size);
    this.page.set(0);
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
