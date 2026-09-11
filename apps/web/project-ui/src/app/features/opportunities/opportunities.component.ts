import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MonitorService } from '../../core/services/monitor.service';
import { ScrapedListingResponse, MatchTier } from '../../core/models/listing.model';
import { Vendor } from '../../core/models/monitor.model';
import { UiPageHeaderComponent } from '../../shared/components/ui-page-header/ui-page-header.component';
import { UiCardComponent } from '../../shared/components/ui-card/ui-card.component';
import { UiBadgeComponent } from '../../shared/components/ui-badge/ui-badge.component';
import { UiStatePanelComponent } from '../../shared/components/ui-state-panel/ui-state-panel.component';
import { UiPaginationComponent } from '../../shared/components/ui-pagination/ui-pagination.component';

type ListingState = 'loading' | 'error' | 'ready';
type SortKey = 'recent' | 'score' | 'priceAsc' | 'priceDesc';

const BATCH_SIZE = 200;

@Component({
  selector: 'app-opportunities',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    UiPageHeaderComponent,
    UiCardComponent,
    UiBadgeComponent,
    UiStatePanelComponent,
    UiPaginationComponent,
  ],
  templateUrl: './opportunities.component.html',
})
export class OpportunitiesComponent implements OnInit {
  private monitorService = inject(MonitorService);

  state = signal<ListingState>('loading');
  listings = signal<ScrapedListingResponse[]>([]);

  // Filtros
  keyword = signal('');
  vendor = signal<Vendor | ''>('');
  tier = signal<MatchTier | ''>('');
  deliveryOnly = signal(false);
  minPrice = signal<number | null>(null);
  maxPrice = signal<number | null>(null);
  sort = signal<SortKey>('recent');

  // Paginação client-side
  page = signal(0);
  pageSize = signal(12);

  filtered = computed(() => {
    const kw = this.keyword().trim().toLowerCase();
    const vendor = this.vendor();
    const tier = this.tier();
    const deliveryOnly = this.deliveryOnly();
    const min = this.minPrice();
    const max = this.maxPrice();

    let result = this.listings().filter((item) => {
      if (kw && !`${item.title} ${item.description ?? ''}`.toLowerCase().includes(kw)) return false;
      if (vendor && item.vendor !== vendor) return false;
      if (tier && item.matchTier !== tier) return false;
      if (deliveryOnly && !item.hasDelivery) return false;
      if (min != null && item.currentPrice < min) return false;
      if (max != null && item.currentPrice > max) return false;
      return true;
    });

    switch (this.sort()) {
      case 'score':
        result = [...result].sort((a, b) => Number(b.matchScore) - Number(a.matchScore));
        break;
      case 'priceAsc':
        result = [...result].sort((a, b) => a.currentPrice - b.currentPrice);
        break;
      case 'priceDesc':
        result = [...result].sort((a, b) => b.currentPrice - a.currentPrice);
        break;
      default:
        result = [...result].sort(
          (a, b) => new Date(b.lastSeenAt ?? 0).getTime() - new Date(a.lastSeenAt ?? 0).getTime(),
        );
    }

    return result;
  });

  totalPages = computed(() => Math.max(1, Math.ceil(this.filtered().length / this.pageSize())));
  paged = computed(() => {
    const start = this.page() * this.pageSize();
    return this.filtered().slice(start, start + this.pageSize());
  });

  hasFilters = computed(
    () =>
      !!this.keyword() ||
      !!this.vendor() ||
      !!this.tier() ||
      this.deliveryOnly() ||
      this.minPrice() != null ||
      this.maxPrice() != null,
  );

  ngOnInit() {
    this.load();
  }

  load() {
    this.state.set('loading');
    this.monitorService.getAllListings(0, BATCH_SIZE).subscribe({
      next: (res) => {
        this.listings.set(res.content ?? []);
        this.page.set(0);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }

  clearFilters() {
    this.keyword.set('');
    this.vendor.set('');
    this.tier.set('');
    this.deliveryOnly.set(false);
    this.minPrice.set(null);
    this.maxPrice.set(null);
    this.page.set(0);
  }

  onKeyword(event: Event) {
    this.keyword.set((event.target as HTMLInputElement).value);
    this.page.set(0);
  }

  onVendor(event: Event) {
    this.vendor.set((event.target as HTMLSelectElement).value as Vendor | '');
    this.page.set(0);
  }

  onTier(event: Event) {
    this.tier.set((event.target as HTMLSelectElement).value as MatchTier | '');
    this.page.set(0);
  }

  onSort(event: Event) {
    this.sort.set((event.target as HTMLSelectElement).value as SortKey);
  }

  onMinPrice(event: Event) {
    this.minPrice.set(this.toNumber(event));
    this.page.set(0);
  }

  onMaxPrice(event: Event) {
    this.maxPrice.set(this.toNumber(event));
    this.page.set(0);
  }

  onDelivery(event: Event) {
    this.deliveryOnly.set((event.target as HTMLInputElement).checked);
    this.page.set(0);
  }

  onPageChange(page: number) {
    this.page.set(page);
  }

  onPageSizeChange(size: number) {
    this.pageSize.set(size);
    this.page.set(0);
  }

  tierVariant(tier: MatchTier): 'high' | 'medium' | 'low' | 'neutral' {
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

  tierLabel(tier: MatchTier): string {
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

  vendorLabel(vendor: Vendor): string {
    return vendor === 'OLX' ? 'OLX' : 'Mercado Livre';
  }

  formatPrice(value: number): string {
    return (value ?? 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
  }

  private toNumber(event: Event): number | null {
    const value = (event.target as HTMLInputElement).value;
    if (value === '') return null;
    const parsed = Number(value);
    return Number.isNaN(parsed) ? null : parsed;
  }
}
