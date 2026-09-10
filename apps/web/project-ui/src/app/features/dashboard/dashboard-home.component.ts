import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MonitorService } from '../../core/services/monitor.service';
import { WebhookService } from '../../core/services/webhook.service';
import { ScraperQueueStatusResponse } from '../../core/models/monitor.model';
import { UiCardComponent } from '../../shared/components/ui-card/ui-card.component';
import { UiButtonComponent } from '../../shared/components/ui-button/ui-button.component';
import { UiPageHeaderComponent } from '../../shared/components/ui-page-header/ui-page-header.component';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [CommonModule, UiCardComponent, UiButtonComponent, UiPageHeaderComponent, RouterModule],
  template: `
    <app-ui-page-header
      title="Visão geral"
      subtitle="Métricas e status da sua operação de inteligência e scraping."
    >
        <app-ui-button variant="secondary" (clicked)="loadMetrics()">
          <svg class="w-4 h-4 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          Atualizar métricas
        </app-ui-button>
    </app-ui-page-header>

    @if (queueUnavailable()) {
      <div class="mb-6 flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800" role="alert">
        <svg class="mt-0.5 h-5 w-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M12 9v4m0 4h.01M10.3 4.4 2.8 17a2 2 0 0 0 1.7 3h15a2 2 0 0 0 1.7-3L13.7 4.4a2 2 0 0 0-3.4 0Z" />
        </svg>
        <span>Não foi possível consultar o scraper. Os dados da fila estão indisponíveis.</span>
      </div>
    }

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
      <!-- Card: Monitores -->
      <app-ui-card [noPadding]="true">
        <div class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-500 uppercase tracking-wider">
                Total de Monitores
              </p>
              <div class="mt-2 flex items-baseline gap-2">
                @if (!metricUnavailable().monitors) {
                  <span class="text-3xl font-bold text-gray-900">{{ metrics().monitors }}</span>
                } @else {
                  <span class="text-2xl font-semibold text-slate-400">--</span>
                }
              </div>
            </div>
            <div class="p-3 bg-blue-50 rounded-lg">
              <svg
                class="w-6 h-6 text-blue-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                ></path>
              </svg>
            </div>
          </div>
          <div class="mt-4">
            <a routerLink="/monitors" class="text-sm font-medium text-blue-600 hover:text-blue-800"
              >Ver monitores &rarr;</a
            >
          </div>
        </div>
      </app-ui-card>

      <!-- Card: Anúncios (apenas metrica) -->
      <app-ui-card [noPadding]="true">
        <div class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-500 uppercase tracking-wider">
                Anúncios Extraídos
              </p>
              <div class="mt-2 flex items-baseline gap-2">
                @if (!metricUnavailable().listings) {
                  <span class="text-3xl font-bold text-gray-900">{{ metrics().listings }}</span>
                } @else {
                  <span class="text-2xl font-semibold text-slate-400">--</span>
                }
              </div>
            </div>
            <div class="p-3 bg-emerald-50 rounded-lg">
              <svg
                class="w-6 h-6 text-emerald-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"
                ></path>
              </svg>
            </div>
          </div>
          <div class="mt-4">
            <span class="text-sm font-medium text-gray-400">Total capturado</span>
          </div>
        </div>
      </app-ui-card>

      <!-- Card: Webhooks -->
      <app-ui-card [noPadding]="true">
        <div class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-500 uppercase tracking-wider">
                Webhooks Recebidos
              </p>
              <div class="mt-2 flex items-baseline gap-2">
                @if (!metricUnavailable().webhooks) {
                  <span class="text-3xl font-bold text-gray-900">{{ metrics().webhooks }}</span>
                } @else {
                  <span class="text-2xl font-semibold text-slate-400">--</span>
                }
              </div>
            </div>
            <div class="p-3 bg-purple-50 rounded-lg">
              <svg
                class="w-6 h-6 text-purple-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M8 7v8a2 2 0 002 2h6M8 7V5a2 2 0 012-2h4.586a1 1 0 01.707.293l4.414 4.414a1 1 0 01.293.707V15a2 2 0 01-2 2h-2M8 7H6a2 2 0 00-2 2v10a2 2 0 002 2h8a2 2 0 002-2v-2"
                ></path>
              </svg>
            </div>
          </div>
          <div class="mt-4">
            <a routerLink="/events" class="text-sm font-medium text-blue-600 hover:text-blue-800"
              >Ver infraestrutura &rarr;</a
            >
          </div>
        </div>
      </app-ui-card>

      <!-- Card: Fila Scraper Python -->
      <app-ui-card [noPadding]="true">
        <div class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-500 uppercase tracking-wider">
                Fila Scraper Python
              </p>
              <div class="mt-2 flex items-baseline gap-2">
                @if (!queueUnavailable()) {
                  <span class="text-3xl font-bold" [ngClass]="queueStatus().total_pending_jobs > 0 ? 'text-amber-600' : 'text-gray-900'">
                    {{ queueStatus().total_pending_jobs }}
                  </span>
                  <span class="text-xs font-semibold px-2 py-0.5 rounded-full"
                        [ngClass]="queueStatus().total_pending_jobs > 0 ? 'bg-amber-100 text-amber-800' : 'bg-green-100 text-green-800'">
                    {{ queueStatus().total_pending_jobs > 0 ? 'Processando' : 'Ocioso' }}
                  </span>
                } @else {
                  <span class="text-2xl font-semibold text-slate-400">--</span>
                  <span class="rounded-full bg-amber-100 px-2 py-0.5 text-xs font-semibold text-amber-800">Indisponível</span>
                }
              </div>
            </div>
            <div class="p-3 rounded-lg" [ngClass]="queueStatus().total_pending_jobs > 0 ? 'bg-amber-50' : 'bg-cyan-50'">
              <svg
                class="w-6 h-6"
                [ngClass]="queueStatus().total_pending_jobs > 0 ? 'text-amber-600 animate-spin' : 'text-cyan-600'"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"
                ></path>
              </svg>
            </div>
          </div>
           @if (!queueUnavailable()) {
             <div class="mt-4 flex items-center justify-between text-xs text-gray-500">
               <span>Fila: <strong class="text-gray-800">{{ queueStatus().queued_jobs }}</strong> | Rodando: <strong class="text-gray-800">{{ queueStatus().running_jobs }}</strong></span>
               <span>Webhooks: <strong class="text-gray-800">{{ queueStatus().pending_webhooks }}</strong></span>
             </div>
           } @else {
             <div class="mt-4 text-xs font-medium text-amber-700">Dados indisponíveis no momento.</div>
           }
        </div>
      </app-ui-card>
    </div>
  `,
})
export class DashboardHomeComponent implements OnInit {
  private monitorService = inject(MonitorService);
  private webhookService = inject(WebhookService);

  metrics = signal({
    monitors: 0,
    listings: 0,
    webhooks: 0,
  });
  metricUnavailable = signal({ monitors: false, listings: false, webhooks: false });

  queueStatus = signal<ScraperQueueStatusResponse>({
    queued_jobs: 0,
    running_jobs: 0,
    total_pending_jobs: 0,
    pending_webhooks: 0,
    total_success_jobs: 0,
    total_failed_jobs: 0,
  });
  queueUnavailable = signal(false);

  ngOnInit() {
    this.loadMetrics();
  }

  loadMetrics() {
    this.monitorService.getMonitors(0, 1).subscribe({
      next: (res) => {
        this.metrics.update((m) => ({ ...m, monitors: res.totalElements }));
        this.metricUnavailable.update((m) => ({ ...m, monitors: false }));
      },
      error: () => this.markMetricUnavailable('monitors')
    });

    this.monitorService.getAllListings(0, 1).subscribe({
      next: (res) => {
        this.metrics.update((m) => ({ ...m, listings: res.totalElements }));
        this.metricUnavailable.update((m) => ({ ...m, listings: false }));
      },
      error: () => this.markMetricUnavailable('listings')
    });

    this.webhookService.getEvents(0, 1).subscribe({
      next: (res) => {
        this.metrics.update((m) => ({ ...m, webhooks: res.totalElements }));
        this.metricUnavailable.update((m) => ({ ...m, webhooks: false }));
      },
      error: () => this.markMetricUnavailable('webhooks')
    });

    this.monitorService.getScraperQueueStatus().subscribe({
      next: (status) => {
        this.queueUnavailable.set(false);
        this.queueStatus.set(status);
      },
      error: () => this.queueUnavailable.set(true)
    });
  }

  private markMetricUnavailable(metric: 'monitors' | 'listings' | 'webhooks'): void {
    this.metricUnavailable.update((current) => ({ ...current, [metric]: true }));
  }
}
