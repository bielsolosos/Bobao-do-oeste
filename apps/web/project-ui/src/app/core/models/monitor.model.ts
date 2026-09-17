export type Vendor = 'OLX' | 'MERCADO_LIVRE';
export type AnalysisType = 'NONE' | 'SIMPLE' | 'NOTEBOOK';
export type ScrapingFrequency =
  | 'EVERY_MINUTE'
  | 'EVERY_5_MINUTES'
  | 'EVERY_30_MINUTES'
  | 'HOURLY'
  | 'EIGHT_TIMES_DAILY'
  | 'SIX_TIMES_DAILY'
  | 'FOUR_TIMES_DAILY'
  | 'EVERY_6_HOURS'
  | 'DAILY'
  | 'TWICE_DAILY'
  | 'WEEKLY'
  | 'MANUAL';

export interface ScrapingFrequencyOption {
  name: ScrapingFrequency;
  description: string;
  cronExpression?: string | null;
}

export interface MonitorSearchQueryResponse {
  id: string;
  queryTerm: string;
  minPrice?: number;
  maxPrice?: number;
  stateFilter?: string;
  regionFilter?: string;
  requireDelivery?: boolean;
  maxPages?: number;
  active?: boolean;
}

export interface SimpleAnalysisFields {
  prompt?: string;
}

export interface NotebookAnalysisFields {
  minimumRamGb?: number;
  needsDedicatedGpu?: boolean | null;
}

export interface MonitorExpectedSpecs extends SimpleAnalysisFields, NotebookAnalysisFields {
  minPrice?: number;
  maxPrice?: number;
  [key: string]: unknown;
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
  expectedSpecs?: MonitorExpectedSpecs;
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
  analysisTypeFields?: SimpleAnalysisFields | NotebookAnalysisFields;
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
  pageable: {
    pageNumber: number;
    pageSize: number;
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  last: boolean;
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}

export interface ScraperQueueStatusResponse {
  queued_jobs: number;
  running_jobs: number;
  total_pending_jobs: number;
  pending_webhooks: number;
  total_success_jobs: number;
  total_failed_jobs: number;
}
