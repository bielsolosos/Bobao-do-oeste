import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ProductMonitorResponse, ScrapingFrequency, Vendor, AnalysisType } from '../../../core/models/monitor.model';
import { MonitorService } from '../../../core/services/monitor.service';
import { UiBadgeComponent } from '../../../shared/components/ui-badge/ui-badge.component';
import { UiConfirmService } from '../../../shared/components/ui-confirm/ui-confirm.service';
import { UiPaginationComponent } from '../../../shared/components/ui-pagination/ui-pagination.component';
import { UiStatePanelComponent } from '../../../shared/components/ui-state-panel/ui-state-panel.component';
import { UiPageHeaderComponent } from '../../../shared/components/ui-page-header/ui-page-header.component';
import { UiButtonComponent } from '../../../shared/components/ui-button/ui-button.component';
import { UiSkeletonComponent } from '../../../shared/components/ui-skeleton/ui-skeleton.component';
import { UiToastService } from '../../../shared/components/ui-toast/ui-toast.service';

type ListState = 'loading' | 'error' | 'ready';

@Component({
  selector: 'app-monitor-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    UiBadgeComponent,
    UiPaginationComponent,
    UiStatePanelComponent,
    UiPageHeaderComponent,
    UiButtonComponent,
    UiSkeletonComponent,
  ],
  templateUrl: './monitor-list.component.html',
})
export class MonitorListComponent implements OnInit {
  private monitorService = inject(MonitorService);
  private toast = inject(UiToastService);
  private confirmService = inject(UiConfirmService);

  state = signal<ListState>('loading');
  monitors = signal<ProductMonitorResponse[]>([]);
  busyId = signal<string | null>(null);

  page = signal(0);
  pageSize = signal(10);
  totalElements = signal(0);
  totalPages = signal(0);

  hasResults = computed(
    () => this.state() === 'ready' && this.monitors().length > 0,
  );
  isEmpty = computed(
    () => this.state() === 'ready' && this.monitors().length === 0,
  );

  ngOnInit() {
    this.loadMonitors();
  }

  loadMonitors() {
    this.state.set('loading');
    this.monitorService
      .getMonitors(this.page(), this.pageSize())
      .subscribe({
        next: (res) => {
          this.monitors.set(res.content ?? []);
          this.totalElements.set(res.totalElements ?? 0);
          this.totalPages.set(res.totalPages ?? 0);
          this.page.set(res.number ?? 0);
          this.state.set('ready');
        },
        error: () => this.state.set('error'),
      });
  }

  retry() {
    this.loadMonitors();
  }

  onPageChange(page: number) {
    this.page.set(page);
    this.loadMonitors();
  }

  onPageSizeChange(size: number) {
    this.pageSize.set(size);
    this.page.set(0);
    this.loadMonitors();
  }

  deactivate(monitor: ProductMonitorResponse) {
    this.busyId.set(monitor.id);
    this.monitorService.deactivateMonitor(monitor.id).subscribe({
      next: () => {
        this.toast.success('Pausado', `O monitor "${monitor.name}" foi pausado.`);
        this.busyId.set(null);
        this.loadMonitors();
      },
      error: () => {
        this.toast.error('Erro', 'Falha ao pausar o monitor.');
        this.busyId.set(null);
      },
    });
  }

  activate(monitor: ProductMonitorResponse) {
    this.busyId.set(monitor.id);
    this.monitorService.activateMonitor(monitor.id).subscribe({
      next: () => {
        this.toast.success('Retomado', `O monitor "${monitor.name}" voltará a buscar.`);
        this.busyId.set(null);
        this.loadMonitors();
      },
      error: () => {
        this.toast.error('Erro', 'Falha ao reativar o monitor.');
        this.busyId.set(null);
      },
    });
  }

  async deleteMonitor(monitor: ProductMonitorResponse) {
    const confirmed = await this.confirmService.confirm({
      title: 'Excluir monitor',
      message: `Deseja realmente excluir "${monitor.name}"? Esta ação é irreversível.`,
      confirmText: 'Excluir',
      cancelText: 'Cancelar',
      isDestructive: true,
    });
    if (!confirmed) return;

    this.busyId.set(monitor.id);
    this.monitorService.deleteMonitor(monitor.id).subscribe({
      next: () => {
        this.toast.success('Excluído', 'Monitor excluído com sucesso.');
        this.busyId.set(null);
        this.loadMonitors();
      },
      error: () => {
        this.toast.error('Erro', 'Falha ao excluir o monitor.');
        this.busyId.set(null);
      },
    });
  }

  vendorLabel(vendor: Vendor): string {
    return vendor === 'OLX' ? 'OLX' : 'Mercado Livre';
  }

  analysisLabel(type: AnalysisType): string {
    switch (type) {
      case 'SIMPLE':
        return 'IA simples';
      case 'NOTEBOOK':
        return 'IA para notebooks';
      default:
        return 'Sem IA';
    }
  }

  frequencyLabel(frequency: ScrapingFrequency): string {
    const labels: Record<ScrapingFrequency, string> = {
      EVERY_MINUTE: 'A cada minuto',
      EVERY_5_MINUTES: 'A cada 5 min',
      EVERY_30_MINUTES: 'A cada 30 min',
      HOURLY: 'A cada hora',
      EVERY_6_HOURS: 'A cada 6 horas',
      DAILY: 'Diário',
      TWICE_DAILY: '2x ao dia',
      WEEKLY: 'Semanal',
      MANUAL: 'Manual',
    };
    return labels[frequency] ?? frequency;
  }
}
