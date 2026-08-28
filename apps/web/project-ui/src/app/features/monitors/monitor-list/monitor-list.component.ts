import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MonitorService } from '../../../core/services/monitor.service';
import { ProductMonitorResponse } from '../../../core/models/monitor.model';
import { UiButtonComponent } from '../../../shared/components/ui-button/ui-button.component';
import { UiCardComponent } from '../../../shared/components/ui-card/ui-card.component';
import { UiBadgeComponent } from '../../../shared/components/ui-badge/ui-badge.component';
import { UiConfirmService } from '../../../shared/components/ui-confirm/ui-confirm.service';
import { UiConfirmComponent } from '../../../shared/components/ui-confirm/ui-confirm.component';
import { UiToastService } from '../../../shared/components/ui-toast/ui-toast.service';

@Component({
  selector: 'app-monitor-list',
  standalone: true,
  imports: [CommonModule, RouterModule, UiButtonComponent, UiCardComponent, UiBadgeComponent, UiConfirmComponent, DatePipe],
  template: `
    <app-ui-confirm-dialog></app-ui-confirm-dialog>

    <div class="mb-6 flex justify-between items-end">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">Monitores de Produtos</h1>
        <p class="text-sm text-gray-500 mt-1">Gerencie suas buscas automatizadas nos marketplaces.</p>
      </div>
      <a routerLink="/monitors/new" class="inline-flex items-center justify-center px-4 py-2 border border-transparent font-medium rounded-lg text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 text-sm shadow-sm transition-colors">
        <svg class="-ml-1 mr-2 h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path></svg>
        Novo Monitor
      </a>
    </div>

    <app-ui-card [noPadding]="true">
      <div *ngIf="isLoading()" class="text-center py-12">
        <svg class="animate-spin mx-auto h-8 w-8 text-blue-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle></svg>
        <p class="mt-4 text-sm text-gray-500">Carregando monitores...</p>
      </div>

      <div *ngIf="!isLoading() && monitors().length === 0" class="text-center py-16">
        <svg class="mx-auto h-12 w-12 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"></path></svg>
        <h3 class="mt-2 text-sm font-medium text-gray-900">Nenhum monitor configurado</h3>
        <p class="mt-1 text-sm text-gray-500">Comece criando seu primeiro monitor de produtos.</p>
      </div>

      <div class="overflow-x-auto" *ngIf="!isLoading() && monitors().length > 0">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
            <tr>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Monitor</th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Agendamento</th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Última Busca</th>
              <th scope="col" class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Ações</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-gray-200">
            <tr *ngFor="let m of monitors()" class="hover:bg-gray-50/50 transition-colors">
              <td class="px-6 py-4 whitespace-nowrap">
                <div class="flex items-center">
                  <div>
                    <div class="text-sm font-medium text-gray-900">{{ m.name }}</div>
                    <div class="text-xs text-gray-500 mt-0.5 flex gap-2 items-center">
                      <span class="font-mono text-[10px] bg-gray-100 px-1.5 py-0.5 rounded border border-gray-200">{{ m.targetVendor }}</span>
                      <span>{{ m.analysisType === 'NONE' ? 'Sem IA' : 'Com IA (' + m.analysisType + ')' }}</span>
                    </div>
                  </div>
                </div>
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <app-ui-badge [variant]="m.active ? 'success' : 'neutral'">
                  {{ m.active ? 'Buscando' : 'Pausado' }}
                </app-ui-badge>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {{ m.frequency }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                <span *ngIf="m.lastScrapedAt">{{ m.lastScrapedAt | date:'dd/MM HH:mm' }}</span>
                <span *ngIf="!m.lastScrapedAt" class="text-gray-400 italic">Nunca coletado</span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium space-x-2">
                <a [routerLink]="['/monitors', m.id]" class="inline-flex items-center text-blue-600 hover:text-blue-900 bg-blue-50 hover:bg-blue-100 px-2.5 py-1.5 rounded transition-colors">
                  Resultados
                </a>
                
                <a [routerLink]="['/monitors/edit', m.id]" class="inline-flex items-center text-gray-600 hover:text-gray-900 bg-gray-100 hover:bg-gray-200 px-2.5 py-1.5 rounded transition-colors" title="Editar config">
                  Editar
                </a>

                <button *ngIf="m.active" (click)="deactivate(m.id)" class="inline-flex items-center text-amber-600 hover:text-amber-900 bg-amber-50 hover:bg-amber-100 px-2.5 py-1.5 rounded transition-colors" title="Pausar busca">
                  Pausar
                </button>
                <button *ngIf="!m.active" (click)="activate(m.id)" class="inline-flex items-center text-emerald-600 hover:text-emerald-900 bg-emerald-50 hover:bg-emerald-100 px-2.5 py-1.5 rounded transition-colors" title="Retomar busca">
                  Retomar
                </button>

                <button (click)="deleteMonitor(m.id, m.name)" class="inline-flex items-center text-red-600 hover:text-red-900 bg-red-50 hover:bg-red-100 px-2.5 py-1.5 rounded transition-colors">
                  <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </app-ui-card>
  `
})
export class MonitorListComponent implements OnInit {
  private monitorService = inject(MonitorService);
  private toast = inject(UiToastService);
  
  monitors = signal<ProductMonitorResponse[]>([]);
  isLoading = signal<boolean>(true);

  ngOnInit() {
    this.loadMonitors();
  }

  loadMonitors() {
    this.isLoading.set(true);
    this.monitorService.getMonitors(0, 50).subscribe({
      next: (res) => {
        this.monitors.set(res.content);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.toast.error('Erro', 'Não foi possível carregar a lista de monitores.');
        this.isLoading.set(false);
      }
    });
  }

  deactivate(id: string) {
    // Executa a pausa direto (não requer confirmacao tao grave)
    this.monitorService.deactivateMonitor(id).subscribe({
      next: () => {
        this.toast.success('Pausado', 'O monitor foi pausado com sucesso.');
        this.loadMonitors();
      },
      error: (err) => {
        console.error('Erro de request PATCH deactivate:', err);
        this.toast.error('Erro', 'Falha ao pausar monitor.');
      }
    });
  }

  activate(id: string) {
    this.monitorService.activateMonitor(id).subscribe({
      next: () => {
        this.toast.success('Retomado', 'O monitor foi reativado e voltará a buscar.');
        this.loadMonitors();
      },
      error: (err) => {
        console.error('Erro de request PATCH activate:', err);
        this.toast.error('Erro', 'Falha ao ativar monitor.');
      }
    });
  }

  deleteMonitor(id: string, name: string) {
    const confirmed = window.confirm(`Atenção! Deseja realmente excluir o monitor "${name}"? Esta ação é irreversível.`);
    
    if (confirmed) {
      this.monitorService.deleteMonitor(id).subscribe({
        next: () => {
          this.toast.success('Excluído', 'Monitor excluído com sucesso.');
          this.loadMonitors();
        },
        error: (err) => {
          console.error('Erro de request DELETE:', err);
          this.toast.error('Erro', 'Falha ao excluir monitor.');
        }
      });
    }
  }
}
