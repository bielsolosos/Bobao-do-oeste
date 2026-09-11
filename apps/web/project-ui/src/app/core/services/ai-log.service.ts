import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';
import { PageResponse } from '../models/monitor.model';
import { AiAnalysisLogResponse } from '../models/ai-log.model';

@Injectable({
  providedIn: 'root'
})
export class AiLogService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/ai-logs`;

  getAllAiLogs(
    page = 0,
    size = 20,
    filters?: { status?: 'SUCCESS' | 'ERROR'; q?: string },
  ): Observable<PageResponse<AiAnalysisLogResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters?.status) params = params.set('status', filters.status);
    if (filters?.q?.trim()) params = params.set('q', filters.q.trim());
    return this.http.get<PageResponse<AiAnalysisLogResponse>>(this.apiUrl, { params });
  }

  getMonitorAiLogs(monitorId: string, page = 0, size = 20): Observable<PageResponse<AiAnalysisLogResponse>> {
    return this.http.get<PageResponse<AiAnalysisLogResponse>>(`${environment.apiUrl}/product-monitors/${monitorId}/ai-logs?page=${page}&size=${size}`);
  }

  getLogsByListingId(listingId: string, page = 0, size = 20): Observable<PageResponse<AiAnalysisLogResponse>> {
    return this.http.get<PageResponse<AiAnalysisLogResponse>>(`${this.apiUrl}/by-listing/${listingId}?page=${page}&size=${size}`);
  }
}
