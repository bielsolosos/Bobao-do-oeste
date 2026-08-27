import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { MonitorService } from '../../core/services/monitor.service';
import { ScrapedListingResponse } from '../../core/models/listing.model';

@Component({
  selector: 'app-listings-list',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, DatePipe],
  template: `
    <div class="bg-white rounded-lg shadow-sm border border-gray-100 p-6">
      <div class="flex justify-between items-center mb-6">
        <div>
          <h2 class="text-xl font-semibold text-gray-800">Anúncios Extraídos & Análise de IA</h2>
          <p class="text-sm text-gray-500">Listagem de todos os anúncios raspados e classificados pelo Gemini.</p>
        </div>
        <button (click)="loadListings()" class="text-blue-600 hover:text-blue-800 text-sm font-medium flex items-center gap-1">
          🔄 Atualizar
        </button>
      </div>

      <div *ngIf="isLoading()" class="text-center py-12 text-gray-500">
        Carregando anúncios raspados...
      </div>

      <div *ngIf="!isLoading() && listings().length === 0" class="text-center py-12 text-gray-500">
        Nenhum anúncio encontrado até o momento.
      </div>

      <div class="overflow-x-auto" *ngIf="!isLoading() && listings().length > 0">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-gray-50 text-gray-700 border-b">
              <th class="p-4 font-medium">Anúncio & IA Summary</th>
              <th class="p-4 font-medium">Preço</th>
              <th class="p-4 font-medium">Plataforma</th>
              <th class="p-4 font-medium">Localização</th>
              <th class="p-4 font-medium">Match IA</th>
              <th class="p-4 font-medium">Última visualização</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let item of listings()" class="border-b hover:bg-gray-50 transition-colors">
              <td class="p-4 max-w-sm">
                <a [href]="item.url" target="_blank" rel="noopener noreferrer" class="font-medium text-blue-600 hover:underline line-clamp-2">
                  {{ item.title }}
                </a>
                <div *ngIf="item.extractedSpecs?.summary" class="mt-1 text-xs text-gray-600 italic bg-slate-50 p-1.5 rounded border border-slate-100">
                  💡 {{ item.extractedSpecs.summary }}
                </div>
                <div class="text-xs text-gray-400 mt-1.5 flex items-center gap-2">
                  <span>Monitor: <b>{{ item.productMonitorName || 'Geral' }}</b></span>
                  <span *ngIf="item.hasDelivery" class="text-green-700 bg-green-50 px-1.5 py-0.5 rounded text-[10px] font-medium">🚚 Entrega</span>
                </div>
              </td>
              <td class="p-4">
                <div class="font-semibold text-gray-800">{{ item.currentPrice | currency:'BRL':'symbol':'1.2-2' }}</div>
                <div *ngIf="item.originalPrice && item.originalPrice > item.currentPrice" class="text-xs text-gray-400 line-through">
                  {{ item.originalPrice | currency:'BRL':'symbol':'1.2-2' }}
                </div>
              </td>
              <td class="p-4">
                <span class="px-2 py-1 bg-slate-100 text-slate-700 rounded text-xs font-medium">
                  {{ item.vendor }}
                </span>
              </td>
              <td class="p-4 text-sm text-gray-600">
                {{ item.city || '--' }} <span *ngIf="item.state">({{ item.state }})</span>
              </td>
              <td class="p-4">
                <div class="flex flex-col gap-1">
                  <span [ngClass]="getTierBadgeClass(item.matchTier)" class="px-2.5 py-1 rounded-full text-xs font-semibold inline-flex items-center gap-1 w-fit">
                    {{ getTierLabel(item.matchTier) }}
                  </span>
                  <span *ngIf="item.matchScore > 0" class="text-[11px] text-gray-500 font-medium pl-1">
                    Score: {{ item.matchScore }}%
                  </span>
                </div>
              </td>
              <td class="p-4 text-xs text-gray-500">
                {{ item.lastSeenAt | date:'dd/MM/yyyy HH:mm' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `
})
export class ListingsListComponent implements OnInit {
  private monitorService = inject(MonitorService);

  listings = signal<ScrapedListingResponse[]>([]);
  isLoading = signal<boolean>(true);

  ngOnInit() {
    this.loadListings();
  }

  loadListings() {
    this.isLoading.set(true);
    this.monitorService.getAllListings(0, 50).subscribe({
      next: (res) => {
        this.listings.set(res.content);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erro ao carregar anúncios', err);
        this.isLoading.set(false);
      }
    });
  }

  getTierBadgeClass(tier: string): string {
    switch (tier) {
      case 'HIGH':
        return 'bg-emerald-100 text-emerald-800 border border-emerald-200';
      case 'MEDIUM':
        return 'bg-amber-100 text-amber-800 border border-amber-200';
      case 'LOW':
        return 'bg-orange-100 text-orange-800 border border-orange-200';
      default:
        return 'bg-gray-100 text-gray-600 border border-gray-200';
    }
  }

  getTierLabel(tier: string): string {
    switch (tier) {
      case 'HIGH':
        return '🔥 Alta Relevância';
      case 'MEDIUM':
        return '⭐ Média Relevância';
      case 'LOW':
        return '⚠️ Baixa Relevância';
      default:
        return '⚪ Sem Match';
    }
  }
}
