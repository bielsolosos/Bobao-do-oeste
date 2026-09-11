import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MonitorService } from '../../../core/services/monitor.service';
import { AiLogService } from '../../../core/services/ai-log.service';
import { ProductMonitorResponse } from '../../../core/models/monitor.model';
import { ScrapedListingResponse, MatchTier } from '../../../core/models/listing.model';
import { AiAnalysisLogResponse } from '../../../core/models/ai-log.model';
import { UiCardComponent } from '../../../shared/components/ui-card/ui-card.component';
import { UiBadgeComponent } from '../../../shared/components/ui-badge/ui-badge.component';
import { UiTabsComponent, TabItem } from '../../../shared/components/ui-tabs/ui-tabs.component';
import { UiPaginationComponent } from '../../../shared/components/ui-pagination/ui-pagination.component';
import { UiStatePanelComponent } from '../../../shared/components/ui-state-panel/ui-state-panel.component';
import { UiDialogComponent } from '../../../shared/components/ui-dialog/ui-dialog.component';
import { UiButtonComponent } from '../../../shared/components/ui-button/ui-button.component';
import { UiPageHeaderComponent } from '../../../shared/components/ui-page-header/ui-page-header.component';
import { UiCodePanelComponent } from '../../../shared/components/ui-code-panel/ui-code-panel.component';
import { UiToastService } from '../../../shared/components/ui-toast/ui-toast.service';

type LoadState = 'loading' | 'error' | 'ready';

@Component({
  selector: 'app-monitor-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    UiCardComponent,
    UiBadgeComponent,
    UiTabsComponent,
    UiPaginationComponent,
    UiStatePanelComponent,
    UiDialogComponent,
    UiButtonComponent,
    UiPageHeaderComponent,
    UiCodePanelComponent,
  ],
  templateUrl: './monitor-detail.component.html',
})
export class MonitorDetailComponent implements OnInit {
  private monitorService = inject(MonitorService);
  private aiLogService = inject(AiLogService);
  private route = inject(ActivatedRoute);
  public router = inject(Router);
  private toast = inject(UiToastService);

  monitorId = '';
  monitor = signal<ProductMonitorResponse | null>(null);
  monitorState = signal<LoadState>('loading');

  listings = signal<ScrapedListingResponse[]>([]);
  listingsState = signal<LoadState>('loading');
  brokenImages = signal<Set<string>>(new Set());

  aiLogs = signal<AiAnalysisLogResponse[]>([]);
  aiLogsState = signal<LoadState>('loading');

  listingKeyword = signal('');
  listingTier = signal<MatchTier | ''>('');
  deliveryOnly = signal(false);
  currentSort = signal('lastSeenAt,desc');

  page = signal(0);
  pageSize = signal(24);
  totalElements = signal(0);
  totalPages = signal(0);

  activeTabId = 'listings';

  isListingAiLogsModalOpen = signal(false);
  selectedListingLogs = signal<AiAnalysisLogResponse[]>([]);
  isLoadingListingLogs = signal(false);
  selectedListingTitle = signal('');

  hasAiAnalysis = computed(() => {
    const m = this.monitor();
    return m !== null && m.analysisType !== 'NONE';
  });

  visibleTabs = computed<TabItem[]>(() => {
    const tabs: TabItem[] = [
      {
        id: 'listings',
        label: 'Anúncios capturados',
        icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"></path></svg>',
      },
    ];
    if (this.hasAiAnalysis()) {
      tabs.push({
        id: 'ai-logs',
        label: 'Logs de IA',
        icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z"></path></svg>',
      });
    }
    tabs.push({
      id: 'config',
      label: 'Configuração',
      icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path><path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path></svg>',
    });
    return tabs;
  });

  ngOnInit() {
    this.monitorId = this.route.snapshot.paramMap.get('id') || '';
    const tab = this.route.snapshot.queryParamMap.get('tab');
    if (tab) this.activeTabId = tab;

    if (this.monitorId) {
      this.loadMonitorInfo();
      this.loadListings();
    }
  }

  loadMonitorInfo() {
    this.monitorState.set('loading');
    this.monitorService.getMonitorById(this.monitorId).subscribe({
      next: (m) => {
        this.monitor.set(m);
        this.monitorState.set('ready');
      },
      error: () => this.monitorState.set('error'),
    });
  }

  loadListings() {
    this.listingsState.set('loading');
    this.monitorService
      .getMonitorListings(this.monitorId, this.page(), this.pageSize(), {
        q: this.listingKeyword(),
        tier: this.listingTier(),
        deliveryOnly: this.deliveryOnly(),
        sort: this.currentSort(),
      })
      .subscribe({
        next: (res) => {
          this.listings.set(res.content ?? []);
          this.totalElements.set(res.totalElements ?? 0);
          this.totalPages.set(res.totalPages ?? 0);
          this.page.set(res.number ?? 0);
          this.listingsState.set('ready');
        },
        error: () => this.listingsState.set('error'),
      });
  }

  loadAiLogs() {
    this.aiLogsState.set('loading');
    this.aiLogService.getMonitorAiLogs(this.monitorId, 0, 50).subscribe({
      next: (res) => {
        this.aiLogs.set(res.content ?? []);
        this.aiLogsState.set('ready');
      },
      error: () => this.aiLogsState.set('error'),
    });
  }

  onTabChange(tabId: string) {
    this.activeTabId = tabId;
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tab: tabId },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });

    if (tabId === 'ai-logs' && this.aiLogsState() !== 'ready' && this.hasAiAnalysis()) {
      this.loadAiLogs();
    }
  }

  onListingKeyword(event: Event) {
    this.listingKeyword.set((event.target as HTMLInputElement).value);
    this.page.set(0);
    this.loadListings();
  }

  onListingTier(event: Event) {
    this.listingTier.set((event.target as HTMLSelectElement).value as MatchTier | '');
    this.page.set(0);
    this.loadListings();
  }

  onDelivery(event: Event) {
    this.deliveryOnly.set((event.target as HTMLInputElement).checked);
    this.page.set(0);
    this.loadListings();
  }

  onSortChange(event: Event) {
    this.currentSort.set((event.target as HTMLSelectElement).value);
    this.page.set(0);
    this.loadListings();
  }

  onPageChange(page: number) {
    this.page.set(page);
    this.loadListings();
  }

  onPageSizeChange(size: number) {
    this.pageSize.set(size);
    this.page.set(0);
    this.loadListings();
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

  getTierLabel(tier: MatchTier): string {
    switch (tier) {
      case 'HIGH':
        return 'Alta relevância';
      case 'MEDIUM':
        return 'Média relevância';
      case 'LOW':
        return 'Baixa relevância';
      default:
        return 'Sem análise';
    }
  }

  formatJson(obj: unknown): string {
    if (!obj || (typeof obj === 'object' && Object.keys(obj as object).length === 0)) {
      return 'Nenhuma configuração específica cadastrada.';
    }
    return JSON.stringify(obj, null, 2);
  }

  openListingAiLogs(listing: ScrapedListingResponse) {
    this.selectedListingTitle.set(listing.title);
    this.isListingAiLogsModalOpen.set(true);
    this.isLoadingListingLogs.set(true);
    this.selectedListingLogs.set([]);

    this.aiLogService.getLogsByListingId(listing.id).subscribe({
      next: (res) => {
        this.selectedListingLogs.set(res.content ?? []);
        this.isLoadingListingLogs.set(false);
      },
      error: () => {
        this.isLoadingListingLogs.set(false);
        this.toast.error('Erro', 'Não foi possível carregar o dossiê deste anúncio.');
      },
    });
  }

  closeListingAiLogs() {
    this.isListingAiLogsModalOpen.set(false);
  }
}
