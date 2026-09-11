import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MonitorService } from '../../core/services/monitor.service';
import { WebhookService } from '../../core/services/webhook.service';
import { ScraperQueueStatusResponse } from '../../core/models/monitor.model';
import { UiPageHeaderComponent } from '../../shared/components/ui-page-header/ui-page-header.component';
import { UiSkeletonComponent } from '../../shared/components/ui-skeleton/ui-skeleton.component';

type LoadState = 'loading' | 'ready' | 'error';

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [CommonModule, RouterModule, UiPageHeaderComponent, UiSkeletonComponent],
  templateUrl: './dashboard-home.component.html',
})
export class DashboardHomeComponent implements OnInit {
  private monitorService = inject(MonitorService);
  private webhookService = inject(WebhookService);

  monitors = signal(0);
  listings = signal(0);
  webhooks = signal(0);
  queueStatus = signal<ScraperQueueStatusResponse | null>(null);

  monitorsState = signal<LoadState>('loading');
  listingsState = signal<LoadState>('loading');
  webhooksState = signal<LoadState>('loading');
  queueState = signal<LoadState>('loading');

  lastUpdatedAt = signal<Date | null>(null);
  isRefreshing = signal(false);

  hasNoMonitors = computed(() => this.monitorsState() === 'ready' && this.monitors() === 0);

  queueHealthy = computed(() => {
    const status = this.queueStatus();
    return this.queueState() === 'ready' && status !== null && status.total_pending_jobs === 0;
  });

  ngOnInit() {
    this.loadMetrics();
  }

  loadMetrics() {
    this.isRefreshing.set(true);

    this.monitorsState.set('loading');
    this.monitorService.getMonitors(0, 1).subscribe({
      next: (res) => {
        this.monitors.set(res.totalElements);
        this.monitorsState.set('ready');
        this.markUpdated();
      },
      error: () => this.monitorsState.set('error'),
    });

    this.listingsState.set('loading');
    this.monitorService.getAllListings(0, 1).subscribe({
      next: (res) => {
        this.listings.set(res.totalElements);
        this.listingsState.set('ready');
      },
      error: () => this.listingsState.set('error'),
    });

    this.webhooksState.set('loading');
    this.webhookService.getEvents(0, 1).subscribe({
      next: (res) => {
        this.webhooks.set(res.totalElements);
        this.webhooksState.set('ready');
      },
      error: () => this.webhooksState.set('error'),
    });

    this.queueState.set('loading');
    this.monitorService.getScraperQueueStatus().subscribe({
      next: (status) => {
        this.queueStatus.set(status);
        this.queueState.set('ready');
        this.markUpdated();
      },
      error: () => this.queueState.set('error'),
    });
  }

  private markUpdated() {
    this.lastUpdatedAt.set(new Date());
    this.isRefreshing.set(false);
  }
}
