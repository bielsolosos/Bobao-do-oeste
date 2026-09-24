import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import {
  BrandDistributionResponse,
  MetricsOverviewResponse,
  PriceDistributionResponse,
  TierMetricsResponse,
  TimelineMetricsResponse,
} from '../../core/models/metrics.model';
import { ScraperQueueStatusResponse } from '../../core/models/monitor.model';
import { MonitorMetricsService } from '../../core/services/monitor-metrics.service';
import { MonitorService } from '../../core/services/monitor.service';
import { WebhookService } from '../../core/services/webhook.service';
import { ChartOptionsUtil } from '../../core/utils/chart-options.util';
import { UiButtonComponent } from '../../shared/components/ui-button/ui-button.component';
import { UiChartComponent } from '../../shared/components/ui-chart/ui-chart.component';
import { UiPageHeaderComponent } from '../../shared/components/ui-page-header/ui-page-header.component';
import { UiStatePanelComponent } from '../../shared/components/ui-state-panel/ui-state-panel.component';
import { UiToastService } from '../../shared/components/ui-toast/ui-toast.service';

type LoadState = 'loading' | 'ready' | 'error';

//MUUUUUUUUUUUUUITA repetição de código. Esse cara pode literalmente ser uma série de dumb ccomponentes euqnaot essa tela é o orquestrador principal.
//Ainda irei fazer o refactor (quando eu tiver saco) para corrigir esse código vibe codded.
@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    UiPageHeaderComponent,
    UiStatePanelComponent,
    UiButtonComponent,
    UiChartComponent,
  ],
  templateUrl: './dashboard-home.component.html',
})
export class DashboardHomeComponent implements OnInit {
  private monitorService = inject(MonitorService);
  private metricsService = inject(MonitorMetricsService);
  private webhookService = inject(WebhookService);
  private toast = inject(UiToastService);

  monitors = signal(0);
  queueStatus = signal<ScraperQueueStatusResponse | null>(null);

  // Modular Metrics Signals
  overview = signal<MetricsOverviewResponse | null>(null);
  tiers = signal<TierMetricsResponse | null>(null);
  prices = signal<PriceDistributionResponse | null>(null);
  brands = signal<BrandDistributionResponse | null>(null);
  timeline = signal<TimelineMetricsResponse | null>(null);

  // State Signals
  monitorsState = signal<LoadState>('loading');
  overviewState = signal<LoadState>('loading');
  tiersState = signal<LoadState>('loading');
  pricesState = signal<LoadState>('loading');
  brandsState = signal<LoadState>('loading');
  timelineState = signal<LoadState>('loading');
  queueState = signal<LoadState>('loading');

  lastUpdatedAt = signal<Date | null>(null);
  isRefreshing = signal(false);

  hasNoMonitors = computed(() => this.monitorsState() === 'ready' && this.monitors() === 0);

  // ECharts Computed Options
  donutOptions = computed(() => {
    const t = this.tiers();
    return t ? ChartOptionsUtil.buildDonutOptions(t) : null;
  });

  priceOptions = computed(() => {
    const p = this.prices();
    return p ? ChartOptionsUtil.buildPriceHistogramOptions(p) : null;
  });

  brandOptions = computed(() => {
    const b = this.brands();
    return b ? ChartOptionsUtil.buildBrandBarOptions(b) : null;
  });

  timelineOptions = computed(() => {
    const tl = this.timeline();
    return tl ? ChartOptionsUtil.buildTimelineOptions(tl) : null;
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

    this.overviewState.set('loading');
    this.metricsService.getOverview().subscribe({
      next: (res) => {
        this.overview.set(res);
        this.overviewState.set('ready');
        this.markUpdated();
      },
      error: () => this.overviewState.set('error'),
    });

    this.tiersState.set('loading');
    this.metricsService.getTiers().subscribe({
      next: (res) => {
        this.tiers.set(res);
        this.tiersState.set('ready');
        this.markUpdated();
      },
      error: () => this.tiersState.set('error'),
    });

    this.pricesState.set('loading');
    this.metricsService.getPrices().subscribe({
      next: (res) => {
        this.prices.set(res);
        this.pricesState.set('ready');
        this.markUpdated();
      },
      error: () => this.pricesState.set('error'),
    });

    this.brandsState.set('loading');
    this.metricsService.getBrands().subscribe({
      next: (res) => {
        this.brands.set(res);
        this.brandsState.set('ready');
        this.markUpdated();
      },
      error: () => this.brandsState.set('error'),
    });

    this.timelineState.set('loading');
    this.metricsService.getTimeline(undefined, 14).subscribe({
      next: (res) => {
        this.timeline.set(res);
        this.timelineState.set('ready');
        this.markUpdated();
      },
      error: () => this.timelineState.set('error'),
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

  private pendingRequests = 0;
  private requestFinished() {
    this.pendingRequests = Math.max(0, this.pendingRequests - 1);
    if (this.pendingRequests === 0) {
      this.lastUpdatedAt.set(new Date());
      this.isRefreshing.set(false);
    }
  }
  private markUpdated() {
    this.pendingRequests += 1;
    queueMicrotask(() => this.requestFinished());
  }
}
