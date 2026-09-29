export interface AiAnalysisLogResponse {
  id: string;
  productMonitorId?: string;
  productMonitorName?: string;
  scrapingExecutionId?: string;
  requestId?: string;
  jobId?: string;
  modelName: string;
  vendor: string;
  itemsCount: number;
  systemPrompt?: string;
  userPrompt?: string;
  rawResponse?: string;
  status: 'SUCCESS' | 'ERROR';
  durationMs?: number;
  errorMessage?: string;
  promptTokens?: number;
  generationTokens?: number;
  totalTokens?: number;
  createdAt: string;
}
