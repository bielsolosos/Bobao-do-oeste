export type Vendor = 'OLX' | 'MERCADO_LIVRE';
export type AnalysisType = 'NONE' | 'SIMPLE' | 'NOTEBOOK';
export type ScrapingFrequency = 'EVERY_MINUTE' | 'EVERY_5_MINUTES' | 'EVERY_30_MINUTES' | 'HOURLY' | 'EVERY_6_HOURS' | 'DAILY' | 'TWICE_DAILY' | 'WEEKLY' | 'MANUAL';

export interface MonitorSearchQueryResponse {
  id: string;
  keyword: string;
}

export interface ProductMonitorResponse {
  id: string;
  name: string;
  description?: string;
  analysisType: AnalysisType;
  targetVendor: Vendor;
  active: boolean;
  frequency: ScrapingFrequency;
  cronExpression?: string;
  expectedSpecs?: any;
  searchQueries: MonitorSearchQueryResponse[];
  lastScrapedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ProductMonitorRequest {
  name: string;
  description?: string;
  vendor: Vendor;
  analysisType: AnalysisType;
  analysisTypeFields: any; // Pode ser { prompt: string } ou { minimumRamGb: number, ... }
  searchKeywords: string[];
  minPrice?: number;
  maxPrice?: number;
  stateFilter?: string;
  regionFilter?: string;
  requireDelivery?: boolean;
  frequency: ScrapingFrequency;
}

export interface PageResponse<T> {
  content: T[];
  pageable: any;
  last: boolean;
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}
