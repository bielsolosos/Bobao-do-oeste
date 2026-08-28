import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MonitorService } from '../../../core/services/monitor.service';
import { ProductMonitorResponse } from '../../../core/models/monitor.model';
import { ScrapedListingResponse } from '../../../core/models/listing.model';
import { AiAnalysisLogResponse } from '../../../core/models/ai-log.model';
import { UiTabsComponent, TabItem } from '../../../shared/components/ui-tabs/ui-tabs.component';
import { UiCardComponent } from '../../../shared/components/ui-card/ui-card.component';
import { UiBadgeComponent } from '../../../shared/components/ui-badge/ui-badge.component';

@Component({
  selector: 'app-monitor-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, UiTabsComponent, UiCardComponent, UiBadgeComponent, CurrencyPipe, DatePipe],
  template: `
    <div class="mb-6 flex items-center justify-between">
      <div>
        <div class="flex items-center gap-3">
          <h1 class="text-2xl font-bold text-gray-900">{{ monitor()?.name || 'Carregando Monitor...' }}</h1>
          <app-ui-badge *ngIf="monitor()" [variant]="monitor()?.active ? 'success' : 'neutral'">
            {{ monitor()?.active ? 'Buscando' : 'Pausado' }}
          </app-ui-badge>
        </div>
        <p class="text-sm text-gray-500 mt-2 font-mono flex items-center gap-4">
          <span>Plataforma: <b>{{ monitor()?.targetVendor }}</b></span>
          <span>IA: <b>{{ monitor()?.analysisType === 'NONE' ? 'Sem IA' : monitor()?.analysisType }}</b></span>
          <span class="text-blue-600 bg-blue-50 px-2 py-0.5 rounded">
            Última busca: <b>{{ monitor()?.lastScrapedAt ? (monitor()?.lastScrapedAt | date:'dd/MM/yyyy HH:mm') : 'Nunca coletado' }}</b>
          </span>
        </p>
      </div>
      <div class="flex gap-2">
        <a routerLink="/monitors" class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50">
          Voltar
        </a>
        <a *ngIf="monitor()" [routerLink]="['/monitors/edit', monitor()?.id]" class="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700">
          Editar Configurações
        </a>
      </div>
    </div>

    <!-- Tabs Dinâmicas (Inteligentes) -->
    <app-ui-tabs 
      [tabs]="visibleTabs()" 
      [activeTabId]="activeTabId" 
      (tabChange)="onTabChange($event)"
      class="mb-6">
    </app-ui-tabs>

    <!-- ABA 1: Anúncios -->
    <div *ngIf="activeTabId === 'listings'" class="space-y-4 animate-fade-in">
      <app-ui-card [noPadding]="true">
        <div *ngIf="isLoadingListings()" class="p-12 text-center text-gray-500">Buscando anúncios...</div>
        
        <div *ngIf="!isLoadingListings() && listings().length === 0" class="p-12 text-center text-gray-500">
          Nenhum anúncio encontrado para este monitor ainda.
        </div>

        <div class="overflow-x-auto" *ngIf="!isLoadingListings() && listings().length > 0">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Produto</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Preço</th>
                <!-- Coluna de IA Oculta se análise for NONE -->
                <th *ngIf="hasAiAnalysis()" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Match IA</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Local/Data</th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr *ngFor="let item of listings()">
                <td class="px-6 py-4">
                  <div class="flex items-start gap-4">
                    <div class="flex-shrink-0 h-12 w-12 bg-gray-100 rounded-md overflow-hidden border border-gray-200">
                      <img *ngIf="item.images && item.images.length > 0" [src]="item.images[0]" class="h-full w-full object-cover">
                      <div *ngIf="!item.images || item.images.length === 0" class="h-full w-full flex items-center justify-center text-gray-400">
                        <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path></svg>
                      </div>
                    </div>
                    <div>
                      <a [href]="item.url" target="_blank" class="text-sm font-medium text-blue-600 hover:underline line-clamp-2">
                        {{ item.title }}
                      </a>
                      <div *ngIf="hasAiAnalysis() && item.extractedSpecs?.summary" class="mt-1 text-xs text-gray-500 italic bg-gray-50 p-2 rounded border border-gray-100">
                        {{ item.extractedSpecs.summary }}
                      </div>
                      <span *ngIf="item.hasDelivery" class="inline-flex mt-1 items-center gap-1 text-[10px] font-medium text-green-700 bg-green-50 px-1.5 py-0.5 rounded">
                        🚚 Entrega Disponível
                      </span>
                    </div>
                  </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm font-bold text-gray-900">{{ item.currentPrice | currency:'BRL':'symbol':'1.2-2' }}</div>
                  <div *ngIf="item.originalPrice && item.originalPrice > item.currentPrice" class="text-xs text-gray-400 line-through">
                    {{ item.originalPrice | currency:'BRL':'symbol':'1.2-2' }}
                  </div>
                </td>
                
                <!-- Match IA Oculto se NONE -->
                <td *ngIf="hasAiAnalysis()" class="px-6 py-4 whitespace-nowrap">
                  <app-ui-badge [variant]="getTierBadge(item.matchTier)">
                    {{ getTierLabel(item.matchTier) }}
                  </app-ui-badge>
                  <div class="text-[10px] text-gray-500 mt-1" *ngIf="item.matchScore > 0">Score: {{ item.matchScore }}%</div>
                </td>

                <td class="px-6 py-4 whitespace-nowrap text-xs text-gray-500">
                  <div>{{ item.city || 'Desconhecido' }} ({{ item.state || '-' }})</div>
                  <div class="mt-0.5">{{ item.lastSeenAt | date:'dd/MM HH:mm' }}</div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </app-ui-card>
    </div>

    <!-- ABA 2: Logs IA (Dinâmica) -->
    <div *ngIf="activeTabId === 'ai-logs' && hasAiAnalysis()" class="space-y-4 animate-fade-in">
      <app-ui-card [noPadding]="true">
        <div *ngIf="isLoadingAiLogs()" class="p-12 text-center text-gray-500">Buscando histórico da IA...</div>
        
        <div *ngIf="!isLoadingAiLogs() && aiLogs().length === 0" class="p-12 text-center text-gray-500">
          Nenhuma análise de IA rodou para este monitor ainda.
        </div>

        <div class="overflow-x-auto" *ngIf="!isLoadingAiLogs() && aiLogs().length > 0">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Data</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Itens Analisados</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Duração</th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr *ngFor="let log of aiLogs()">
                <td class="px-6 py-4 whitespace-nowrap">
                  <app-ui-badge [variant]="log.status === 'SUCCESS' ? 'success' : 'danger'">
                    {{ log.status }}
                  </app-ui-badge>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {{ log.createdAt | date:'dd/MM/yyyy HH:mm:ss' }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900 font-medium">
                  {{ log.itemsCount }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                  {{ log.durationMs }}ms
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </app-ui-card>
    </div>

    <!-- ABA 3: Configurações -->
    <div *ngIf="activeTabId === 'config'" class="space-y-4 animate-fade-in">
      <app-ui-card title="Detalhes Estruturais">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <h4 class="text-sm font-medium text-gray-500 uppercase tracking-wider mb-2">Palavras-chave Atuais</h4>
            <div class="flex flex-wrap gap-2">
              <span *ngFor="let q of monitor()?.searchQueries" class="px-2 py-1 bg-blue-50 text-blue-700 text-xs font-semibold rounded border border-blue-200">
                {{ q.keyword }}
              </span>
            </div>
            
            <h4 class="text-sm font-medium text-gray-500 uppercase tracking-wider mt-6 mb-2">Filtros de Preço</h4>
            <p class="text-sm text-gray-700 font-mono">
              Min: {{ monitor()?.expectedSpecs?.minPrice || 'Sem limite' }} | Máx: {{ monitor()?.expectedSpecs?.maxPrice || 'Sem limite' }}
            </p>
          </div>

          <div *ngIf="hasAiAnalysis()">
            <h4 class="text-sm font-medium text-gray-500 uppercase tracking-wider mb-2">
              Regras da Inteligência Artificial ({{ monitor()?.analysisType }})
            </h4>
            <pre class="bg-slate-900 text-emerald-400 p-4 rounded-md text-xs font-mono overflow-auto max-h-48 shadow-inner">{{ formatJson(monitor()?.expectedSpecs) }}</pre>
          </div>
          
          <div *ngIf="!hasAiAnalysis()">
            <h4 class="text-sm font-medium text-gray-500 uppercase tracking-wider mb-2">Inteligência Artificial</h4>
            <div class="p-4 bg-gray-50 border border-gray-200 rounded-md text-sm text-gray-500 italic">
              Este monitor realiza apenas coleta bruta sem passar por análise semântica da IA.
            </div>
          </div>
        </div>
      </app-ui-card>
    </div>
  `,
  styles: [`
    .animate-fade-in { animation: fadeIn 0.2s ease-in-out forwards; }
    @keyframes fadeIn { from { opacity: 0; transform: translateY(5px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class MonitorDetailComponent implements OnInit {
  private monitorService = inject(MonitorService);
  private route = inject(ActivatedRoute);

  monitorId: string = '';
  monitor = signal<ProductMonitorResponse | null>(null);
  
  listings = signal<ScrapedListingResponse[]>([]);
  isLoadingListings = signal(false);

  aiLogs = signal<AiAnalysisLogResponse[]>([]);
  isLoadingAiLogs = signal(false);

  activeTabId: string = 'listings';

  // Computed Signal para esconder aba de Logs se a análise for NONE
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
      error: (err) => console.error(err)
    });
  }

  loadListings() {
    this.isLoadingListings.set(true);
    this.monitorService.getMonitorListings(this.monitorId, 0, 50).subscribe({
      next: (res) => {
        this.listings.set(res.content);
        this.isLoadingListings.set(false);
      }
    });
  }

  loadAiLogs() {
    this.isLoadingAiLogs.set(true);
    this.monitorService.getMonitorAiLogs(this.monitorId, 0, 50).subscribe({
      next: (res) => {
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

  formatJson(obj: any): string {
    if (!obj || Object.keys(obj).length === 0) return 'Nenhuma regra específica cadastrada.';
    // Remove os campos de preço do RAW se eles vierem misturados nas expectedSpecs
    const cleanObj = { ...obj };
    delete cleanObj.minPrice;
    delete cleanObj.maxPrice;
    
    return JSON.stringify(cleanObj, null, 2);
  }
}
