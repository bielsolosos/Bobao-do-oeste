import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AiLogService } from '../../core/services/ai-log.service';
import { AiAnalysisLogResponse } from '../../core/models/ai-log.model';
import { UiCardComponent } from '../../shared/components/ui-card/ui-card.component';
import { UiBadgeComponent } from '../../shared/components/ui-badge/ui-badge.component';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-ai-logs-list',
  standalone: true,
  imports: [CommonModule, DatePipe, UiCardComponent, UiBadgeComponent, RouterModule],
  template: `
    <div class="mb-6 flex justify-between items-end">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Auditoria & Logs de Chamadas da IA</h1>
        <p class="text-sm text-gray-500 mt-1">Histórico detalhado de prompts, respostas do Gemini, latência e consumo.</p>
      </div>
      <button (click)="loadLogs()" class="inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50">
        <svg class="-ml-1 mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path></svg>
        Atualizar
      </button>
    </div>

    <app-ui-card [noPadding]="true">
      <div *ngIf="isLoading()" class="p-12 text-center text-gray-500">
        Carregando histórico de IA...
      </div>

      <div *ngIf="!isLoading() && logs().length === 0" class="p-12 text-center text-gray-500">
        Nenhum registro de chamada de IA encontrado até o momento.
      </div>

      <div class="overflow-x-auto" *ngIf="!isLoading() && logs().length > 0">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Monitor</th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Modelo</th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Lote / Desempenho</th>
              <th scope="col" class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Ações</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr *ngFor="let log of logs()" class="hover:bg-gray-50 transition-colors">
              <td class="px-6 py-4 whitespace-nowrap">
                <app-ui-badge [variant]="log.status === 'SUCCESS' ? 'success' : 'danger'">
                  {{ log.status }}
                </app-ui-badge>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <a [routerLink]="['/monitors', log.productMonitorId]" class="text-sm font-medium text-gray-900 hover:text-blue-600 hover:underline">
                  {{ log.productMonitorName || 'Geral' }}
                </a>
                <div class="text-xs text-gray-500 mt-1">{{ log.createdAt | date:'short' }}</div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span class="px-2 py-0.5 bg-slate-100 border border-slate-200 rounded text-xs font-mono text-slate-700">
                  {{ log.modelName }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="text-sm text-gray-900"><span class="font-bold">{{ log.itemsCount }}</span> anúncios avaliados</div>
                <div class="text-xs text-gray-500 mt-1">Latência: {{ formatDuration(log.durationMs) }}</div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                <button (click)="openDetailModal(log)" class="text-blue-600 hover:text-blue-900 bg-blue-50 hover:bg-blue-100 px-3 py-1.5 rounded-md transition-colors font-semibold">
                  Ver Payload
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </app-ui-card>

    <!-- Modal de Detalhes -->
    <div *ngIf="selectedLog()" class="relative z-[100]" aria-labelledby="modal-title" role="dialog" aria-modal="true">
      <div class="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" (click)="closeDetailModal()"></div>

      <div class="fixed inset-0 z-10 w-screen overflow-y-auto">
        <div class="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
          
          <div class="relative transform overflow-hidden rounded-lg bg-white text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-4xl">
            <div class="bg-white px-4 pb-4 pt-5 sm:p-6 sm:pb-4 border-b border-gray-200">
              <div class="flex justify-between items-start">
                <div>
                  <h3 class="text-lg font-semibold leading-6 text-gray-900" id="modal-title">Detalhes da Execução de IA</h3>
                  <p class="text-sm text-gray-500 mt-1">Modelo: <span class="font-mono text-xs">{{ selectedLog()?.modelName }}</span> | Latência: {{ formatDuration(selectedLog()?.durationMs) }}</p>
                </div>
                <button (click)="closeDetailModal()" class="rounded-md bg-white text-gray-400 hover:text-gray-500 focus:outline-none">
                  <span class="sr-only">Close</span>
                  <svg class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>

            <div class="bg-gray-50 px-4 py-5 sm:p-6 space-y-6 overflow-y-auto max-h-[60vh]">
              
              <div *ngIf="selectedLog()?.errorMessage" class="rounded-md bg-red-50 p-4 border border-red-200">
                <div class="flex">
                  <div class="flex-shrink-0">
                    <svg class="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" /></svg>
                  </div>
                  <div class="ml-3">
                    <h3 class="text-sm font-medium text-red-800">Falha na execução</h3>
                    <div class="mt-2 text-sm text-red-700">
                      <p>{{ selectedLog()?.errorMessage }}</p>
                    </div>
                  </div>
                </div>
              </div>

              <div>
                <h4 class="text-sm font-semibold text-gray-900 mb-2">System Prompt</h4>
                <pre class="bg-slate-900 text-slate-300 p-4 rounded-lg overflow-x-auto text-[11px] font-mono leading-relaxed max-h-48 whitespace-pre-wrap">{{ selectedLog()?.systemPrompt }}</pre>
              </div>

              <div>
                <h4 class="text-sm font-semibold text-gray-900 mb-2">User Prompt (Lote de Itens)</h4>
                <pre class="bg-slate-900 text-emerald-400 p-4 rounded-lg overflow-x-auto text-[11px] font-mono leading-relaxed max-h-48 whitespace-pre-wrap">{{ selectedLog()?.userPrompt }}</pre>
              </div>

              <div *ngIf="selectedLog()?.rawResponse">
                <h4 class="text-sm font-semibold text-gray-900 mb-2">Resposta Raw / JSON</h4>
                <pre class="bg-slate-900 text-amber-300 p-4 rounded-lg overflow-x-auto text-[11px] font-mono leading-relaxed max-h-96 whitespace-pre-wrap">{{ formatJson(selectedLog()?.rawResponse) }}</pre>
              </div>

            </div>
            
            <div class="bg-white px-4 py-3 sm:flex sm:flex-row-reverse sm:px-6 border-t border-gray-200">
              <button type="button" (click)="closeDetailModal()" class="mt-3 inline-flex w-full justify-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50 sm:mt-0 sm:w-auto">
                Fechar
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class AiLogsListComponent implements OnInit {
  private aiLogService = inject(AiLogService);

  logs = signal<AiAnalysisLogResponse[]>([]);
  isLoading = signal<boolean>(true);
  selectedLog = signal<AiAnalysisLogResponse | null>(null);

  ngOnInit() {
    this.loadLogs();
  }

  loadLogs() {
    this.isLoading.set(true);
    this.aiLogService.getAllAiLogs(0, 50).subscribe({
      next: (res) => {
        this.logs.set(res.content);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erro ao carregar logs de IA', err);
        this.isLoading.set(false);
      }
    });
  }

  openDetailModal(log: AiAnalysisLogResponse) {
    this.selectedLog.set(log);
  }

  closeDetailModal() {
    this.selectedLog.set(null);
  }

  formatDuration(ms?: number): string {
    if (ms == null) return '--';
    if (ms >= 1000) return (ms / 1000).toFixed(2) + 's';
    return ms + 'ms';
  }

  formatJson(rawJson?: string): string {
    if (!rawJson) return '';
    try {
      const parsed = JSON.parse(rawJson);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return rawJson;
    }
  }
}
