export interface MetricsOverviewResponse {
  totalListings: number;
  highRelevanceCount: number;
  minPrice: number | null;
  maxPrice: number | null;
  avgPrice: number | null;
  lastScrapedAt: string | null;
}

export interface TierMetricsResponse {
  high: number;
  medium: number;
  low: number;
  none: number;
  total: number;
}

export interface PriceBucket {
  label: string;
  min: number;
  max: number | null;
  count: number;
}

export interface PriceDistributionResponse {
  buckets: PriceBucket[];
}

export interface BrandItem {
  brand: string;
  count: number;
}

export interface BrandDistributionResponse {
  brands: BrandItem[];
}

export interface TimelinePoint {
  date: string;
  count: number;
}

export interface TimelineMetricsResponse {
  points: TimelinePoint[];
}
