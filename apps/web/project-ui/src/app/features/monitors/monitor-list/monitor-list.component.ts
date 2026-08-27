import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MonitorService } from '../../../core/services/monitor.service';
import { ProductMonitorResponse } from '../../../core/models/monitor.model';

@Component({
  selector: 'app-monitor-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="bg-white rounded-lg shadow-sm border border-gray-100 p-6">
      <div class="flex justify-between items-center mb-6">
        <h2 class="text-xl font-semibold text-gray-800">Meus Monitores</h2>
        <a routerLink="/monitors/new" class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded font-medium transition-colors">
          + Novo Monitor
        </a>
      </div>

      <div *ngIf="isLoading()" class="text-center py-8 text-gray-500">
        Carregando monitores...
      </div>

      <div *ngIf="!isLoading() && monitors().length === 0" class="text-center py-8 text-gray-500">
        Nenhum monitor encontrado. Crie um para começar!
      </div>

      <div class="overflow-x-auto" *ngIf="!isLoading() && monitors().length > 0">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-gray-50 text-gray-700 border-b">
              <th class="p-4 font-medium">Nome</th>
              <th class="p-4 font-medium">Plataforma</th>
              <th class="p-4 font-medium">Frequência</th>
              <th class="p-4 font-medium">Status</th>
              <th class="p-4 font-medium text-right">Ações</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let m of monitors()" class="border-b hover:bg-gray-50 transition-colors">
              <td class="p-4">
                <div class="font-medium text-gray-800">{{ m.name }}</div>
                <div class="text-xs text-gray-500">{{ m.analysisType }}</div>
              </td>
              <td class="p-4">
                <span class="px-2 py-1 bg-gray-200 text-gray-700 rounded text-xs">
                  {{ m.targetVendor }}
                </span>
              </td>
              <td class="p-4 text-sm text-gray-600">{{ m.frequency }}</td>
              <td class="p-4">
                <span *ngIf="m.active" class="px-2 py-1 bg-green-100 text-green-700 rounded-full text-xs font-medium">Ativo</span>
                <span *ngIf="!m.active" class="px-2 py-1 bg-red-100 text-red-700 rounded-full text-xs font-medium">Inativo</span>
              </td>
              <td class="p-4 flex gap-2 justify-end">
                <button 
                  *ngIf="m.active"
                  (click)="deactivate(m.id)"
                  title="Pausar busca"
                  class="text-orange-600 hover:text-orange-800 font-medium px-2 py-1 rounded hover:bg-orange-50 transition-colors">
                  ⏸ Pausar
                </button>
                <button 
                  *ngIf="!m.active"
                  (click)="activate(m.id)"
                  title="Retomar busca"
                  class="text-green-600 hover:text-green-800 font-medium px-2 py-1 rounded hover:bg-green-50 transition-colors">
                  ▶ Retomar
                </button>
                <button 
                  (click)="deleteMonitor(m.id)"
                  title="Excluir"
                  class="text-red-600 hover:text-red-800 font-medium px-2 py-1 rounded hover:bg-red-50 transition-colors ml-2">
                  🗑 Excluir
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `
})
export class MonitorListComponent implements OnInit {
  private monitorService = inject(MonitorService);
  
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
        console.error('Erro ao buscar monitores', err);
        this.isLoading.set(false);
      }
    });
  }

  deactivate(id: string) {
    if(confirm('Tem certeza que deseja pausar este monitor? Ele parará de buscar novos anúncios.')) {
      this.monitorService.deactivateMonitor(id).subscribe({
        next: () => this.loadMonitors(),
        error: (err) => {
          alert('Erro ao pausar monitor.');
          console.error(err);
        }
      });
    }
  }

  activate(id: string) {
    if(confirm('Deseja retomar a busca para este monitor?')) {
      this.monitorService.activateMonitor(id).subscribe({
        next: () => this.loadMonitors(),
        error: (err) => {
          alert('Erro ao retomar monitor.');
          console.error(err);
        }
      });
    }
  }

  deleteMonitor(id: string) {
    if(confirm('Atenção! Esta ação é irreversível. Deseja realmente excluir este monitor?')) {
      this.monitorService.deleteMonitor(id).subscribe({
        next: () => this.loadMonitors(),
        error: (err) => {
          alert('Erro ao excluir monitor.');
          console.error(err);
        }
      });
    }
  }
}
