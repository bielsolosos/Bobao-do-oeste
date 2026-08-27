export type MatchTier = 'HIGH' | 'MEDIUM' | 'LOW' | 'NONE';

export interface ScrapedListingResponse {
  id: string;
  productMonitorId?: string;
  productMonitorName?: string;
  vendor: 'OLX' | 'MERCADO_LIVRE' | 'ENJOEI';
  vendorListingId: string;
  title: string;
  url: string;
  description?: string;
  currentPrice: number;
  originalPrice?: number;
  state?: string;
  city?: string;
  neighborhood?: string;
  hasDelivery: boolean;
  deliveryType?: string;
  images?: any;
  matchTier: MatchTier;
  matchScore: number;
  extractedSpecs?: any;
  publishedAt?: string;
  firstSeenAt: string;
  lastSeenAt: string;
}
