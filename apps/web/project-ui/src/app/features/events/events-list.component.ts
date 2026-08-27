import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { WebhookService } from '../../core/services/webhook.service';
import { WebhookEventSummaryResponse } from '../../core/models/webhook.model';

@Component({
  selector: 'app-events-list',
  standalone: true,
  imports: [CommonModule, DatePipe],
  template: `
    <div class="bg-white rounded-lg shadow-sm border border-gray-100 p-6">
      <div class="flex justify-between items-center mb-6">
        <h2 class="text-xl font-semibold text-gray-800">Log de Eventos (Webhooks)</h2>
        <button (click)="loadEvents()" class="text-blue-600 hover:text-blue-800 text-sm font-medium flex items-center gap-1">
          🔄 Atualizar
        </button>
      </div>

      <div *ngIf="isLoading()" class="text-center py-8 text-gray-500">
        Carregando histórico de eventos...
      </div>

      <div *ngIf="!isLoading() && events().length === 0" class="text-center py-8 text-gray-500">
        Nenhum evento processado ainda.
      </div>

      <div class="overflow-x-auto" *ngIf="!isLoading() && events().length > 0">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-gray-50 text-gray-700 border-b">
              <th class="p-4 font-medium">Data/Hora</th>
              <th class="p-4 font-medium">Job ID</th>
              <th class="p-4 font-medium">Tipo</th>
              <th class="p-4 font-medium">Status</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let e of events()" class="border-b hover:bg-gray-50 transition-colors">
              <td class="p-4 text-sm text-gray-600">
                {{ e.createdAt | date:'dd/MM/yyyy HH:mm:ss' }}
              </td>
              <td class="p-4 font-mono text-xs text-gray-500">
                {{ e.jobId || 'N/A' }}
              </td>
              <td class="p-4 text-sm text-gray-700">
                {{ e.eventType }}
              </td>
              <td class="p-4">
                <span *ngIf="e.status === 'PROCESSED'" class="px-2 py-1 bg-green-100 text-green-700 rounded-full text-xs font-medium">Sucesso</span>
                <span *ngIf="e.status === 'FAILED'" class="px-2 py-1 bg-red-100 text-red-700 rounded-full text-xs font-medium">Falha</span>
                <span *ngIf="e.status === 'PENDING'" class="px-2 py-1 bg-yellow-100 text-yellow-700 rounded-full text-xs font-medium">Pendente</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `
})
export class EventsListComponent implements OnInit {
  private webhookService = inject(WebhookService);
  
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
        console.error('Erro ao buscar eventos', err);
        this.isLoading.set(false);
      }
    });
  }
}
