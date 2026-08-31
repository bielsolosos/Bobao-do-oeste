import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ProductMonitorResponse } from '../../../core/models/monitor.model';
import { MonitorService } from '../../../core/services/monitor.service';
import { UiBadgeComponent } from '../../../shared/components/ui-badge/ui-badge.component';
import { UiCardComponent } from '../../../shared/components/ui-card/ui-card.component';
import { UiConfirmComponent } from '../../../shared/components/ui-confirm/ui-confirm.component';
import { UiPaginationComponent } from '../../../shared/components/ui-pagination/ui-pagination.component';
import { UiToastService } from '../../../shared/components/ui-toast/ui-toast.service';

@Component({
  selector: 'app-monitor-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    UiCardComponent,
    UiBadgeComponent,
    UiConfirmComponent,
    UiPaginationComponent,
  ],
  templateUrl: './monitor-list.component.html',
})
export class MonitorListComponent implements OnInit {
  private monitorService = inject(MonitorService);
  private toast = inject(UiToastService);

  monitors = signal<ProductMonitorResponse[]>([]);
  isLoading = signal<boolean>(true);

  // Variáveis de paginação
  currentPage = signal<number>(0);
  totalPages = signal<number>(0);
  totalElements = signal<number>(0);
  pageSize = signal<number>(10);

  ngOnInit() {
    this.loadMonitors();
  }

  loadMonitors(page: number = this.currentPage()) {
    this.isLoading.set(true);
    this.monitorService.getMonitors(page, this.pageSize()).subscribe({
      next: (res: any) => {
        this.monitors.set(res.content);
        this.currentPage.set(res.number);
        this.totalPages.set(res.totalPages);
        this.totalElements.set(res.totalElements);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.toast.error('Erro', 'Não foi possível carregar a lista de monitores.');
        this.isLoading.set(false);
      },
    });
  }

  onPageSizeChange(size: number) {
    this.pageSize.set(size);
    this.loadMonitors(0);
  }

  onPageChange(page: number) {
    this.loadMonitors(page);
  }

  deactivate(id: string) {
    this.monitorService.deactivateMonitor(id).subscribe({
      next: () => {
        this.toast.success('Pausado', 'O monitor foi pausado com sucesso.');
        this.loadMonitors();
      },
      error: (err) => {
        console.error('Erro de request PATCH deactivate:', err);
        this.toast.error('Erro', 'Falha ao pausar monitor.');
      },
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
      },
    });
  }

  deleteMonitor(id: string, name: string) {
    const confirmed = window.confirm(
      `Atenção! Deseja realmente excluir o monitor "${name}"? Esta ação é irreversível.`,
    );

    if (confirmed) {
      this.monitorService.deleteMonitor(id).subscribe({
        next: () => {
          this.toast.success('Excluído', 'Monitor excluído com sucesso.');
          this.loadMonitors();
        },
        error: (err) => {
          console.error('Erro de request DELETE:', err);
          this.toast.error('Erro', 'Falha ao excluir monitor.');
        },
      });
    }
  }
}
