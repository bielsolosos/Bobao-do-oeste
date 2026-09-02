import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MonitorService } from '../../../core/services/monitor.service';
import { AiLogService } from '../../../core/services/ai-log.service';
import { ProductMonitorResponse } from "../../../core/models/monitor.model";
import { ScrapedListingResponse } from "../../../core/models/listing.model";;
import { AiAnalysisLogResponse } from '../../../core/models/ai-log.model';
import { UiCardComponent } from '../../../shared/components/ui-card/ui-card.component';
import { UiBadgeComponent } from '../../../shared/components/ui-badge/ui-badge.component';
import { UiTabsComponent, TabItem } from '../../../shared/components/ui-tabs/ui-tabs.component';
import { UiPaginationComponent } from '../../../shared/components/ui-pagination/ui-pagination.component';

import { UiToastService } from '../../../shared/components/ui-toast/ui-toast.service';

@Component({
  selector: 'app-monitor-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, DatePipe, UiCardComponent, UiBadgeComponent, UiTabsComponent, UiPaginationComponent],
  templateUrl: './monitor-detail.component.html',
  styles: [`
    .animate-fade-in { animation: fadeIn 0.2s ease-in-out forwards; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(5px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class MonitorDetailComponent implements OnInit {
  private monitorService = inject(MonitorService);
  private aiLogService = inject(AiLogService);
  private route = inject(ActivatedRoute);
  private toast = inject(UiToastService);

  monitorId: string = '';
  monitor = signal<ProductMonitorResponse | null>(null);
  
  listings = signal<ScrapedListingResponse[]>([]);
  isLoadingListings = signal(false);

  // Pagination Listings
  currentPage = signal<number>(0);
  totalPages = signal<number>(0);
  totalElements = signal<number>(0);
  currentSort = signal<string>('lastSeenAt,desc');
  pageSize = signal<number>(50);

  aiLogs = signal<AiAnalysisLogResponse[]>([]);
  isLoadingAiLogs = signal(false);

  activeTabId: string = 'listings';

  hasAiAnalysis = computed(() => {
    const m = this.monitor();
    return m !== null && m.analysisType !== 'NONE';
  });

  visibleTabs = computed<TabItem[]>(() => {
    const tabs: TabItem[] = [
      { id: 'listings', label: 'Anúncios Capturados', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"></path></svg>' }
    ];
    
    if (this.hasAiAnalysis()) {
      tabs.push({ id: 'ai-logs', label: 'Logs de IA', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z"></path></svg>' });
    }
    
    tabs.push({ id: 'config', label: 'Configurações RAW', icon: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path><path d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path></svg>' });
    
    return tabs;
  });

  ngOnInit() {
    this.monitorId = this.route.snapshot.paramMap.get('id') || '';
    if (this.monitorId) {
      this.loadMonitorInfo();
      this.loadListings();
    }
  }

  loadMonitorInfo() {
    this.monitorService.getMonitorById(this.monitorId).subscribe({
      next: (m) => {
        this.monitor.set(m);
      },
      error: (err) => {
        console.error(err);
        this.toast.error('Erro', 'Falha ao carregar informações do monitor.');
      }
    });
  }

  loadListings(page: number = this.currentPage()) {
    this.isLoadingListings.set(true);
    this.monitorService.getMonitorListings(this.monitorId, page, this.pageSize(), this.currentSort()).subscribe({
      next: (res: any) => {
        this.listings.set(res.content);
        this.currentPage.set(res.number);
        this.totalPages.set(res.totalPages);
        this.totalElements.set(res.totalElements);
        this.isLoadingListings.set(false);
      },
      error: (err) => {
        console.error(err);
        this.isLoadingListings.set(false);
      }
    });
  }

  onPageSizeChange(size: number) {
    this.pageSize.set(size);
    this.loadListings(0);
  }

  onSortChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.currentSort.set(value);
    this.loadListings(0);
  }

  onPageChange(page: number) {
    this.loadListings(page);
  }

  loadAiLogs() {
    this.isLoadingAiLogs.set(true);
    this.aiLogService.getMonitorAiLogs(this.monitorId, 0, 50).subscribe({
      next: (res: any) => {
        this.aiLogs.set(res.content);
        this.isLoadingAiLogs.set(false);
      }
    });
  }

  onTabChange(tabId: string) {
    this.activeTabId = tabId;
    if (tabId === 'listings' && this.listings().length === 0) this.loadListings();
    if (tabId === 'ai-logs' && this.aiLogs().length === 0 && this.hasAiAnalysis()) this.loadAiLogs();
  }

  getTierBadge(tier: string): any {
    switch(tier) {
      case 'HIGH': return 'high';
      case 'MEDIUM': return 'medium';
      case 'LOW': return 'low';
      default: return 'neutral';
    }
  }

  getTierLabel(tier: string): string {
    switch (tier) {
      case 'HIGH': return 'Alta Relevância';
      case 'MEDIUM': return 'Média Relevância';
      case 'LOW': return 'Baixa Relevância';
      default: return 'Sem Match';
    }
  }

  isListingAiLogsModalOpen = signal(false);
  selectedListingLogs = signal<AiAnalysisLogResponse[]>([]);
  isLoadingListingLogs = signal(false);

  openListingAiLogs(listingId: string) {
    this.isListingAiLogsModalOpen.set(true);
    this.isLoadingListingLogs.set(true);
    this.selectedListingLogs.set([]);

    this.aiLogService.getLogsByListingId(listingId).subscribe({
      next: (res) => {
        this.selectedListingLogs.set(res.content);
        this.isLoadingListingLogs.set(false);
      },
      error: (err) => {
        console.error(err);
        this.toast.error('Erro', 'Não foi possível carregar os logs da IA para este item.');
        this.isLoadingListingLogs.set(false);
      }
    });
  }

  closeListingAiLogs() {
    this.isListingAiLogsModalOpen.set(false);
  }

  formatJson(obj: any): string {
    if (!obj || Object.keys(obj).length === 0) return 'Nenhuma regra específica cadastrada.';
    const cleanObj = { ...obj };
    delete cleanObj.minPrice;
    delete cleanObj.maxPrice;
    
    return JSON.stringify(cleanObj, null, 2);
  }
}
