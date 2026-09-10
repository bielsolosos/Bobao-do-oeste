import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { WebhookService } from '../../core/services/webhook.service';
import { WebhookEventSummaryResponse } from '../../core/models/webhook.model';
import { UiCardComponent } from '../../shared/components/ui-card/ui-card.component';
import { BadgeVariant, UiBadgeComponent } from '../../shared/components/ui-badge/ui-badge.component';
import { UiButtonComponent } from '../../shared/components/ui-button/ui-button.component';
import { UiToastService } from '../../shared/components/ui-toast/ui-toast.service';
import { UiPaginationComponent } from '../../shared/components/ui-pagination/ui-pagination.component';
import { UiEmptyStateComponent } from '../../shared/components/ui-empty-state/ui-empty-state.component';
import { UiPageHeaderComponent } from '../../shared/components/ui-page-header/ui-page-header.component';
import { UiSkeletonComponent } from '../../shared/components/ui-skeleton/ui-skeleton.component';

@Component({
  selector: 'app-events-list',
  standalone: true,
  imports: [CommonModule, DatePipe, UiCardComponent, UiBadgeComponent, UiButtonComponent, UiPaginationComponent, UiEmptyStateComponent, UiPageHeaderComponent, UiSkeletonComponent],
  templateUrl: './events-list.component.html'
})
export class EventsListComponent implements OnInit {
  private webhookService = inject(WebhookService);
  private toast = inject(UiToastService);
  
  events = signal<WebhookEventSummaryResponse[]>([]);
  isLoading = signal<boolean>(true);

  currentPage = signal<number>(0);
  totalPages = signal<number>(0);
  totalElements = signal<number>(0);
  pageSize = signal<number>(50);

  ngOnInit() {
    this.loadEvents();
  }

  loadEvents(page: number = this.currentPage()) {
    this.isLoading.set(true);
    this.webhookService.getEvents(page, this.pageSize()).subscribe({
      next: (res) => {
        this.events.set(res.content);
        this.currentPage.set(res.number);
        this.totalPages.set(res.totalPages);
        this.totalElements.set(res.totalElements);
        this.isLoading.set(false);
      },
      error: () => {
        this.toast.error('Erro', 'Não foi possível carregar os webhooks.');
        this.isLoading.set(false);
      }
    });
  }

  onPageSizeChange(size: number) {
    this.pageSize.set(size);
    this.loadEvents(0);
  }

  onPageChange(page: number) {
    this.loadEvents(page);
  }

  getStatusVariant(status: string): BadgeVariant {
    switch (status) {
      case 'PROCESSED': return 'success';
      case 'FAILED': return 'danger';
      case 'RECEIVED': 
      case 'PROCESSING': return 'warning';
      case 'DUPLICATE': return 'neutral';
      default: return 'neutral';
    }
  }
}
