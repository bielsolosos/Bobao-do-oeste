import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { WebhookService } from '../../core/services/webhook.service';
import { WebhookEventSummaryResponse } from '../../core/models/webhook.model';
import { UiCardComponent } from '../../shared/components/ui-card/ui-card.component';
import { UiBadgeComponent } from '../../shared/components/ui-badge/ui-badge.component';
import { UiButtonComponent } from '../../shared/components/ui-button/ui-button.component';
import { UiToastService } from '../../shared/components/ui-toast/ui-toast.service';

@Component({
  selector: 'app-events-list',
  standalone: true,
  imports: [CommonModule, DatePipe, UiCardComponent, UiBadgeComponent, UiButtonComponent],
  template: `
    <div class="mb-6 flex justify-between items-end">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Infraestrutura: Webhooks</h1>
        <p class="text-sm text-gray-500 mt-1">Monitoramento de eventos assíncronos que chegaram do motor de scraping (Python).</p>
      </div>
      <app-ui-button (onClick)="loadEvents()" variant="secondary">
        <svg class="-ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
        Atualizar
      </app-ui-button>
    </div>

    <app-ui-card [noPadding]="true">
      <div *ngIf="isLoading()" class="p-12 text-center text-gray-500">
        Carregando histórico de eventos...
      </div>

      <div *ngIf="!isLoading() && events().length === 0" class="p-12 text-center text-gray-500">
        Nenhum evento recebido até o momento.
      </div>

      <div class="overflow-x-auto" *ngIf="!isLoading() && events().length > 0">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Recebido Em</th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Job ID / Request ID</th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tipo (Vendor)</th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status do Ingest</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr *ngFor="let e of events()" class="hover:bg-gray-50/50 transition-colors">
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900 font-medium">
                {{ e.createdAt | date:'dd/MM/yyyy HH:mm:ss' }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-xs font-mono text-gray-800">{{ e.jobId || 'N/A' }}</div>
                <div class="text-[10px] text-gray-500 mt-1">Req: {{ e.requestId || '-' }}</div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                {{ e.eventType }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <app-ui-badge [variant]="getStatusVariant(e.status)">
                  {{ e.status }}
                </app-ui-badge>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </app-ui-card>
  `
})
export class EventsListComponent implements OnInit {
  private webhookService = inject(WebhookService);
  private toast = inject(UiToastService);
  
  events = signal<WebhookEventSummaryResponse[]>([]);
  isLoading = signal<boolean>(true);

  ngOnInit() {
    this.loadEvents();
  }

  loadEvents() {
    this.isLoading.set(true);
    this.webhookService.getEvents(0, 50).subscribe({
      next: (res) => {
        this.events.set(res.content);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.toast.error('Erro', 'Não foi possível carregar os webhooks.');
        this.isLoading.set(false);
      }
    });
  }

  getStatusVariant(status: string): any {
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
