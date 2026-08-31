import { Component, inject, OnInit, signal } from '@angular/core';

import { MonitorService } from '../../core/services/monitor.service';
import { WebhookService } from '../../core/services/webhook.service';
import { UiCardComponent } from '../../shared/components/ui-card/ui-card.component';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [UiCardComponent, RouterModule],
  template: `
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">Visão Geral</h1>
      <p class="text-sm text-gray-500 mt-1">
        Métricas e status geral da sua operação de inteligência.
      </p>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
      <!-- Card: Monitores -->
      <app-ui-card [noPadding]="true">
        <div class="p-6">
          <div class="flex items-center justify-between">
            <div>
              <p class="text-sm font-medium text-gray-500 uppercase tracking-wider">
                Total de Monitores
              </p>
              <div class="mt-2 flex items-baseline gap-2">
                <span class="text-3xl font-bold text-gray-900">{{ metrics().monitors }}</span>
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
                <span class="text-3xl font-bold text-gray-900">{{ metrics().listings }}</span>
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
                <span class="text-3xl font-bold text-gray-900">{{ metrics().webhooks }}</span>
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

  ngOnInit() {
    this.monitorService.getMonitors(0, 1).subscribe((res) => {
      this.metrics.update((m) => ({ ...m, monitors: res.totalElements }));
    });

    this.monitorService.getAllListings(0, 1).subscribe((res) => {
      this.metrics.update((m) => ({ ...m, listings: res.totalElements }));
    });

    this.webhookService.getEvents(0, 1).subscribe((res) => {
      this.metrics.update((m) => ({ ...m, webhooks: res.totalElements }));
    });
  }
}
