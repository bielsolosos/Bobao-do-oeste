import { Vendor } from './monitor.model';

export type MatchTier = 'HIGH' | 'MEDIUM' | 'LOW' | 'NONE';

export interface ExtractedSpecs {
  summary?: string;
  brand?: string;
  model?: string;
  processorBrand?: string;
  processorModel?: string;
  processGeneration?: number;
  processorTier?: string;
  ramSize?: number;
  ramType?: string;
  storageSizeGb?: number;
  diskType?: string;
  screenResolution?: string;
  hasGpu?: boolean;
  isReproved?: boolean;
  reproveReason?: string;
  penalties?: string[];
  processor?: string;
  ramGb?: number;
  storage?: string;
  hasDedicatedGpu?: boolean;
  gpuModel?: string;
  condition?: string;
  screenSize?: string;
  batteryStatus?: string;
  pros?: string[];
  cons?: string[];
  estimatedMarketValue?: number;
  dealVerdict?: string;
}

export interface ScrapedListingResponse {
  id: string;
  productMonitorId: string;
  productMonitorName: string;
  vendor: Vendor;
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
  images?: string[];
  matchTier: MatchTier;
  matchScore: number;
  extractedSpecs?: ExtractedSpecs;
  publishedAt?: string;
  firstSeenAt?: string;
  lastSeenAt?: string;
}
