export type WebhookStatus = 'PENDING' | 'RECEIVED' | 'PROCESSING' | 'PROCESSED' | 'FAILED' | 'DUPLICATE';

export interface WebhookEventSummaryResponse {
  id: string;
  eventType: string;
  requestId: string;
  jobId: string;
  status: WebhookStatus;
  processedAt?: string;
  createdAt: string;
}
