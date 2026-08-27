import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MonitorService } from '../../core/services/monitor.service';
import { AiAnalysisLogResponse } from '../../core/models/ai-log.model';

@Component({
  selector: 'app-ai-logs-list',
  standalone: true,
  imports: [CommonModule, DatePipe],
  template: `
    <div class="bg-white rounded-lg shadow-sm border border-gray-100 p-6">
      <div class="flex justify-between items-center mb-6">
        <div>
          <h2 class="text-xl font-semibold text-gray-800">🧠 Auditoria & Logs de Chamadas da IA</h2>
          <p class="text-sm text-gray-500">Histórico detalhado de prompts, respostas do Gemini, latência e consumo de itens analisados.</p>
        </div>
        <button (click)="loadLogs()" class="text-blue-600 hover:text-blue-800 text-sm font-medium flex items-center gap-1">
          🔄 Atualizar
        </button>
      </div>

      <div *ngIf="isLoading()" class="text-center py-12 text-gray-500">
        Carregando histórico de IA...
      </div>

      <div *ngIf="!isLoading() && logs().length === 0" class="text-center py-12 text-gray-500">
        Nenhum registro de chamada de IA encontrado até o momento.
      </div>

      <div class="overflow-x-auto" *ngIf="!isLoading() && logs().length > 0">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-gray-50 text-gray-700 border-b text-sm">
              <th class="p-4 font-medium">Status</th>
              <th class="p-4 font-medium">Monitor</th>
              <th class="p-4 font-medium">Modelo / Vendor</th>
              <th class="p-4 font-medium">Itens Analisados</th>
              <th class="p-4 font-medium">Latência</th>
              <th class="p-4 font-medium">Data / Hora</th>
              <th class="p-4 font-medium text-right">Detalhes</th>
            </tr>
          </thead>
          <tbody class="text-sm">
            <tr *ngFor="let log of logs()" class="border-b hover:bg-gray-50 transition-colors">
              <td class="p-4">
                <span [ngClass]="log.status === 'SUCCESS' ? 'bg-green-100 text-green-800 border-green-200' : 'bg-red-100 text-red-800 border-red-200'"
                      class="px-2.5 py-1 rounded-full text-xs font-semibold border inline-block">
                  {{ log.status }}
                </span>
              </td>
              <td class="p-4 font-medium text-gray-800">
                {{ log.productMonitorName || 'Geral' }}
              </td>
              <td class="p-4 text-gray-600">
                <span class="px-2 py-0.5 bg-slate-100 rounded text-xs font-mono">{{ log.modelName }}</span>
              </td>
              <td class="p-4 text-gray-600">
                <span class="font-semibold">{{ log.itemsCount }}</span> anúncios
              </td>
              <td class="p-4 text-gray-500">
                {{ formatDuration(log.durationMs) }}
              </td>
              <td class="p-4 text-gray-500 text-xs">
                {{ log.createdAt | date:'dd/MM/yyyy HH:mm:ss' }}
              </td>
              <td class="p-4 text-right">
                <button (click)="openDetailModal(log)"
                        class="px-3 py-1 bg-blue-50 text-blue-600 hover:bg-blue-100 rounded text-xs font-medium transition-colors">
                  🔍 Ver Prompt & JSON
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- MODAL DE DETALHES DO PROMPT / RESPOSTA -->
    <div *ngIf="selectedLog()" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
      <div class="bg-white rounded-xl shadow-2xl max-w-4xl w-full max-h-[90vh] flex flex-col overflow-hidden border border-gray-100">
        <!-- Modal Header -->
        <div class="px-6 py-4 border-b flex justify-between items-center bg-gray-50">
          <div>
            <h3 class="font-semibold text-gray-800 text-lg">Detalhes da Execução de IA (Gemini)</h3>
            <p class="text-xs text-gray-500">Modelo: {{ selectedLog()?.modelName }} | Latência: {{ formatDuration(selectedLog()?.durationMs) }}</p>
          </div>
          <button (click)="closeDetailModal()" class="text-gray-400 hover:text-gray-600 text-xl font-bold p-1">
            ✕
          </button>
        </div>

        <!-- Modal Body -->
        <div class="p-6 overflow-y-auto space-y-4 flex-1 text-xs font-mono">
          <div *ngIf="selectedLog()?.errorMessage" class="bg-red-50 text-red-700 p-3 rounded border border-red-200">
            <b>Erro:</b> {{ selectedLog()?.errorMessage }}
          </div>

          <div>
            <h4 class="font-bold text-gray-700 font-sans text-sm mb-1">System Prompt:</h4>
            <pre class="bg-slate-900 text-slate-100 p-3 rounded-lg overflow-x-auto whitespace-pre-wrap max-h-48 text-[11px]">{{ selectedLog()?.systemPrompt }}</pre>
          </div>

          <div>
            <h4 class="font-bold text-gray-700 font-sans text-sm mb-1">User Prompt (Lote de Anúncios):</h4>
            <pre class="bg-slate-900 text-emerald-400 p-3 rounded-lg overflow-x-auto whitespace-pre-wrap max-h-48 text-[11px]">{{ selectedLog()?.userPrompt }}</pre>
          </div>

          <div *ngIf="selectedLog()?.rawResponse">
            <h4 class="font-bold text-gray-700 font-sans text-sm mb-1">Resposta Retornada (Structured Output JSON):</h4>
            <pre class="bg-slate-900 text-amber-300 p-3 rounded-lg overflow-x-auto whitespace-pre-wrap max-h-60 text-[11px]">{{ formatJson(selectedLog()?.rawResponse) }}</pre>
          </div>
        </div>

        <!-- Modal Footer -->
        <div class="px-6 py-3 border-t bg-gray-50 flex justify-end">
          <button (click)="closeDetailModal()" class="px-4 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 rounded-lg text-xs font-medium">
            Fechar
          </button>
        </div>
      </div>
    </div>
  `
})
export class AiLogsListComponent implements OnInit {
  private monitorService = inject(MonitorService);

  logs = signal<AiAnalysisLogResponse[]>([]);
  isLoading = signal<boolean>(true);
  selectedLog = signal<AiAnalysisLogResponse | null>(null);

  ngOnInit() {
    this.loadLogs();
  }

  loadLogs() {
    this.isLoading.set(true);
    this.monitorService.getAllAiLogs(0, 50).subscribe({
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
