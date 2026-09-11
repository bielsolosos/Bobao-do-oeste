import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ProductMonitorResponse, ScrapingFrequency, Vendor, AnalysisType } from '../../../core/models/monitor.model';
import { MonitorService } from '../../../core/services/monitor.service';
import { UiBadgeComponent } from '../../../shared/components/ui-badge/ui-badge.component';
import { UiCardComponent } from '../../../shared/components/ui-card/ui-card.component';
import { UiConfirmService } from '../../../shared/components/ui-confirm/ui-confirm.service';
import { UiPaginationComponent } from '../../../shared/components/ui-pagination/ui-pagination.component';
import { UiStatePanelComponent } from '../../../shared/components/ui-state-panel/ui-state-panel.component';
import { UiPageHeaderComponent } from '../../../shared/components/ui-page-header/ui-page-header.component';
import { UiIconButtonComponent } from '../../../shared/components/ui-icon-button/ui-icon-button.component';
import { UiToastService } from '../../../shared/components/ui-toast/ui-toast.service';

type ListState = 'loading' | 'error' | 'ready';

@Component({
  selector: 'app-monitor-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    UiCardComponent,
    UiBadgeComponent,
    UiPaginationComponent,
    UiStatePanelComponent,
    UiPageHeaderComponent,
    UiIconButtonComponent,
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

  // Filtros client-side
  keyword = signal('');
  statusFilter = signal<'all' | 'active' | 'paused'>('all');
  vendorFilter = signal<Vendor | ''>('');

  page = signal(0);
  pageSize = signal(10);

  filtered = computed(() => {
    const kw = this.keyword().trim().toLowerCase();
    const status = this.statusFilter();
    const vendor = this.vendorFilter();

    return this.monitors().filter((m) => {
      if (kw && !m.name.toLowerCase().includes(kw)) return false;
      if (status === 'active' && !m.active) return false;
      if (status === 'paused' && m.active) return false;
      if (vendor && m.targetVendor !== vendor) return false;
      return true;
    });
  });

  totalPages = computed(() => Math.max(1, Math.ceil(this.filtered().length / this.pageSize())));
  paged = computed(() => {
    const start = this.page() * this.pageSize();
    return this.filtered().slice(start, start + this.pageSize());
  });

  hasFilters = computed(() => !!this.keyword() || this.statusFilter() !== 'all' || !!this.vendorFilter());

  ngOnInit() {
    this.loadMonitors();
  }

  loadMonitors() {
    this.state.set('loading');
    this.monitorService.getMonitors(0, 200).subscribe({
      next: (res) => {
        this.monitors.set(res.content ?? []);
        this.page.set(0);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }

  onKeyword(event: Event) {
    this.keyword.set((event.target as HTMLInputElement).value);
    this.page.set(0);
  }

  onStatus(event: Event) {
    this.statusFilter.set((event.target as HTMLSelectElement).value as 'all' | 'active' | 'paused');
    this.page.set(0);
  }

  onVendor(event: Event) {
    this.vendorFilter.set((event.target as HTMLSelectElement).value as Vendor | '');
    this.page.set(0);
  }

  clearFilters() {
    this.keyword.set('');
    this.statusFilter.set('all');
    this.vendorFilter.set('');
    this.page.set(0);
  }

  onPageChange(page: number) {
    this.page.set(page);
  }

  onPageSizeChange(size: number) {
    this.pageSize.set(size);
    this.page.set(0);
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
