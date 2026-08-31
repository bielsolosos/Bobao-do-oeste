import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
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

  getAllAiLogs(page = 0, size = 20): Observable<PageResponse<AiAnalysisLogResponse>> {
    return this.http.get<PageResponse<AiAnalysisLogResponse>>(`${this.apiUrl}?page=${page}&size=${size}`);
  }

  getMonitorAiLogs(monitorId: string, page = 0, size = 20): Observable<PageResponse<AiAnalysisLogResponse>> {
    return this.http.get<PageResponse<AiAnalysisLogResponse>>(`${environment.apiUrl}/product-monitors/${monitorId}/ai-logs?page=${page}&size=${size}`);
  }
}
