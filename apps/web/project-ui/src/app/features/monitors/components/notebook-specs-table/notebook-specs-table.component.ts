import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, computed, inject, signal } from '@angular/core';
import { MatchTier, ScrapedListingResponse } from '../../../../core/models/listing.model';
import { UiBadgeComponent } from '../../../../shared/components/ui-badge/ui-badge.component';
import { UiButtonComponent } from '../../../../shared/components/ui-button/ui-button.component';
import { UiCardComponent } from '../../../../shared/components/ui-card/ui-card.component';
import { UiStatePanelComponent } from '../../../../shared/components/ui-state-panel/ui-state-panel.component';
import { UiDialogComponent } from '../../../../shared/components/ui-dialog/ui-dialog.component';
import { UiToastService } from '../../../../shared/components/ui-toast/ui-toast.service';

@Component({
  selector: 'app-notebook-specs-table',
  standalone: true,
  imports: [
    CommonModule,
    UiCardComponent,
    UiBadgeComponent,
    UiButtonComponent,
    UiStatePanelComponent,
    UiDialogComponent,
  ],
  templateUrl: './notebook-specs-table.component.html',
})
export class NotebookSpecsTableComponent {
  private toast = inject(UiToastService);

  @Input() listings: ScrapedListingResponse[] = [];
  @Input() loading = false;
  @Input() monitorName = 'Monitor';
  @Input() hasAiAnalysis = true;

  @Output() openAiLogs = new EventEmitter<ScrapedListingResponse>();

  // Details Modal Signal
  isDetailsModalOpen = signal(false);
  selectedListing = signal<ScrapedListingResponse | null>(null);

  // Filter Signals
  specsKeyword = signal('');
  specsMinRam = signal<number | null>(null);
  specsMinGen = signal<number | null>(null);
  specsOnlyDedicatedGpu = signal(false);
  specsHideReproved = signal(false);
  specsTier = signal<MatchTier | ''>('');

  // Sort Signals
  specsSortColumn = signal<string>('matchScore');
  specsSortDirection = signal<'asc' | 'desc'>('desc');

  // Broken Images tracker
  brokenImages = signal<Set<string>>(new Set());

  hasActiveSpecsFilters = computed(() => {
    return (
      !!this.specsKeyword() ||
      this.specsMinRam() !== null ||
      this.specsMinGen() !== null ||
      this.specsOnlyDedicatedGpu() ||
      this.specsHideReproved() ||
      !!this.specsTier()
    );
  });

  filteredAndSortedSpecsListings = computed(() => {
    let items = [...this.listings];
    const kw = this.specsKeyword().toLowerCase().trim();
    const minRam = this.specsMinRam();
    const minGen = this.specsMinGen();
    const dedicatedGpuOnly = this.specsOnlyDedicatedGpu();
    const hideReproved = this.specsHideReproved();
    const tier = this.specsTier();

    if (kw) {
      items = items.filter((item) => {
        const title = item.title?.toLowerCase() || '';
        const specs = item.extractedSpecs;
        const brand = specs?.brand?.toLowerCase() || '';
        const model = specs?.model?.toLowerCase() || '';
        const cpu = (specs?.processorModel || specs?.processor || '')?.toLowerCase();
        const reason = specs?.reproveReason?.toLowerCase() || '';
        return (
          title.includes(kw) ||
          brand.includes(kw) ||
          model.includes(kw) ||
          cpu.includes(kw) ||
          reason.includes(kw)
        );
      });
    }

    if (minRam !== null) {
      items = items.filter((item) => {
        const ram = item.extractedSpecs?.ramSize ?? item.extractedSpecs?.ramGb ?? 0;
        return ram >= minRam;
      });
    }

    if (minGen !== null) {
      items = items.filter((item) => {
        const gen = item.extractedSpecs?.processGeneration ?? 0;
        return gen >= minGen;
      });
    }

    if (dedicatedGpuOnly) {
      items = items.filter((item) => {
        const specs = item.extractedSpecs;
        return specs?.hasGpu === true || specs?.hasDedicatedGpu === true;
      });
    }

    if (hideReproved) {
      items = items.filter((item) => !item.extractedSpecs?.isReproved);
    }

    if (tier) {
      items = items.filter((item) => item.matchTier === tier);
    }

    // Sort
    const col = this.specsSortColumn();
    const dir = this.specsSortDirection();
    const multiplier = dir === 'asc' ? 1 : -1;

    items.sort((a, b) => {
      const valA = this.getSpecsSortValue(a, col);
      const valB = this.getSpecsSortValue(b, col);

      if (typeof valA === 'number' && typeof valB === 'number') {
        return (valA - valB) * multiplier;
      }
      return (
        String(valA).localeCompare(String(valB), undefined, {
          numeric: true,
          sensitivity: 'base',
        }) * multiplier
      );
    });

    return items;
  });

  toggleSpecsSort(column: string) {
    if (this.specsSortColumn() === column) {
      this.specsSortDirection.update((dir) => (dir === 'asc' ? 'desc' : 'asc'));
    } else {
      this.specsSortColumn.set(column);
      const numericColumns = [
        'currentPrice',
        'matchScore',
        'processGeneration',
        'ramSize',
        'storageSizeGb',
        'hasGpu',
        'isReproved',
      ];
      this.specsSortDirection.set(numericColumns.includes(column) ? 'desc' : 'asc');
    }
  }

  getSpecsSortIcon(column: string): 'asc' | 'desc' | 'none' {
    if (this.specsSortColumn() !== column) return 'none';
    return this.specsSortDirection();
  }

  setFilterMinRam(ram: number | null) {
    this.specsMinRam.update((current) => (current === ram ? null : ram));
  }

  setFilterMinGen(gen: number | null) {
    this.specsMinGen.update((current) => (current === gen ? null : gen));
  }

  onSpecsKeyword(event: Event) {
    this.specsKeyword.set((event.target as HTMLInputElement).value);
  }

  onSpecsTier(event: Event) {
    this.specsTier.set((event.target as HTMLSelectElement).value as MatchTier | '');
  }

  toggleDedicatedGpu() {
    this.specsOnlyDedicatedGpu.update((v) => !v);
  }

  toggleHideReproved() {
    this.specsHideReproved.update((v) => !v);
  }

  resetSpecsFilters() {
    this.specsKeyword.set('');
    this.specsMinRam.set(null);
    this.specsMinGen.set(null);
    this.specsOnlyDedicatedGpu.set(false);
    this.specsHideReproved.set(false);
    this.specsTier.set('');
  }

  onImageError(id: string) {
    this.brokenImages.update((set) => new Set(set).add(id));
  }

  isImageBroken(id: string): boolean {
    return this.brokenImages().has(id);
  }

  getTierBadge(tier: MatchTier): 'high' | 'medium' | 'low' | 'neutral' {
    switch (tier) {
      case 'HIGH':
        return 'high';
      case 'MEDIUM':
        return 'medium';
      case 'LOW':
        return 'low';
      default:
        return 'neutral';
    }
  }

  openDetailsModal(listing: ScrapedListingResponse) {
    this.selectedListing.set(listing);
    this.isDetailsModalOpen.set(true);
  }

  closeDetailsModal() {
    this.isDetailsModalOpen.set(false);
    this.selectedListing.set(null);
  }

  triggerOpenAiLogs(item: ScrapedListingResponse) {
    this.openAiLogs.emit(item);
  }

  exportSpecsToCsv() {
    const list = this.filteredAndSortedSpecsListings();
    if (!list || list.length === 0) {
      this.toast.warning('Atenção', 'Nenhum anúncio para exportar com os filtros atuais.');
      return;
    }

    const headers = [
      'ID',
      'Título',
      'Preço (R$)',
      'Relevância',
      'Score (%)',
      'Marca',
      'Modelo CPU',
      'Geração CPU',
      'RAM (GB)',
      'Tipo RAM',
      'Armazenamento (GB)',
      'Tipo Disco',
      'GPU Dedicada',
      'Modelo GPU',
      'Reprovado',
      'Motivo Reprovação',
      'Cidade',
      'Estado',
      'Link',
    ];

    const escapeCsv = (val: unknown) => {
      if (val === null || val === undefined) return '""';
      const str = String(val).replace(/"/g, '""');
      return `"${str}"`;
    };

    const rows = list.map((item) => {
      const s = item.extractedSpecs;
      return [
        escapeCsv(item.id),
        escapeCsv(item.title),
        escapeCsv(item.currentPrice),
        escapeCsv(item.matchTier),
        escapeCsv(item.matchScore),
        escapeCsv(s?.brand || ''),
        escapeCsv(s?.processorModel || s?.processor || ''),
        escapeCsv(s?.processGeneration ?? ''),
        escapeCsv(s?.ramSize ?? s?.ramGb ?? ''),
        escapeCsv(s?.ramType || ''),
        escapeCsv(s?.storageSizeGb ?? ''),
        escapeCsv(s?.diskType || ''),
        escapeCsv(s?.hasGpu || s?.hasDedicatedGpu ? 'SIM' : 'NÃO'),
        escapeCsv(s?.gpuModel || ''),
        escapeCsv(s?.isReproved ? 'SIM' : 'NÃO'),
        escapeCsv(s?.reproveReason || ''),
        escapeCsv(item.city || ''),
        escapeCsv(item.state || ''),
        escapeCsv(item.url),
      ].join(';');
    });

    const csvContent = [headers.join(';'), ...rows].join('\r\n');
    const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    const filename = `specs-notebooks-${(this.monitorName || 'monitor').toLowerCase().replace(/\s+/g, '-')}-${new Date().toISOString().slice(0, 10)}.csv`;

    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);

    this.toast.success(
      'Exportação concluída',
      `${list.length} registros exportados com sucesso.`
    );
  }

  private getSpecsSortValue(item: ScrapedListingResponse, col: string): string | number {
    const s = item.extractedSpecs;
    switch (col) {
      case 'title':
        return item.title || '';
      case 'currentPrice':
        return item.currentPrice ?? 0;
      case 'matchScore':
        return item.matchScore ?? 0;
      case 'brand':
        return s?.brand || s?.processorBrand || '';
      case 'processorModel':
        return s?.processorModel || s?.processor || '';
      case 'processGeneration':
        return s?.processGeneration ?? -1;
      case 'ramSize':
        return s?.ramSize ?? s?.ramGb ?? -1;
      case 'storageSizeGb':
        return s?.storageSizeGb ?? -1;
      case 'hasGpu':
        return s?.hasGpu || s?.hasDedicatedGpu ? 1 : 0;
      case 'isReproved':
        return s?.isReproved ? 1 : 0;
      case 'lastSeenAt':
        return item.lastSeenAt || '';
      default:
        return item.matchScore ?? 0;
    }
  }
}
