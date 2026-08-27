export type WebhookStatus = 'PENDING' | 'PROCESSED' | 'FAILED';

export interface WebhookEventSummaryResponse {
  id: string;
  eventType: string;
  requestId: string;
  jobId: string;
  status: WebhookStatus;
  processedAt?: string;
  createdAt: string;
}
