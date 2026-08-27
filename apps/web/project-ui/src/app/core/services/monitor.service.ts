import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';
import { ProductMonitorRequest, ProductMonitorResponse, PageResponse } from '../models/monitor.model';
import { ScrapedListingResponse } from '../models/listing.model';
import { AiAnalysisLogResponse } from '../models/ai-log.model';

@Injectable({
  providedIn: 'root'
})
export class MonitorService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/product-monitors`;

  getMonitors(page = 0, size = 10): Observable<PageResponse<ProductMonitorResponse>> {
    return this.http.get<PageResponse<ProductMonitorResponse>>(`${this.apiUrl}?page=${page}&size=${size}`);
  }

  createMonitor(monitor: ProductMonitorRequest): Observable<ProductMonitorResponse> {
    return this.http.post<ProductMonitorResponse>(this.apiUrl, monitor);
  }

  deactivateMonitor(id: string): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/deactivate`, {});
  }

  activateMonitor(id: string): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/activate`, {});
  }

  deleteMonitor(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getAllListings(page = 0, size = 20): Observable<PageResponse<ScrapedListingResponse>> {
    return this.http.get<PageResponse<ScrapedListingResponse>>(`${this.apiUrl}/listings?page=${page}&size=${size}`);
  }

  getMonitorListings(monitorId: string, page = 0, size = 20): Observable<PageResponse<ScrapedListingResponse>> {
    return this.http.get<PageResponse<ScrapedListingResponse>>(`${this.apiUrl}/${monitorId}/listings?page=${page}&size=${size}`);
  }

  getAllAiLogs(page = 0, size = 20): Observable<PageResponse<AiAnalysisLogResponse>> {
    return this.http.get<PageResponse<AiAnalysisLogResponse>>(`${this.apiUrl}/ai-logs?page=${page}&size=${size}`);
  }

  getMonitorAiLogs(monitorId: string, page = 0, size = 20): Observable<PageResponse<AiAnalysisLogResponse>> {
    return this.http.get<PageResponse<AiAnalysisLogResponse>>(`${this.apiUrl}/${monitorId}/ai-logs?page=${page}&size=${size}`);
  }
}
