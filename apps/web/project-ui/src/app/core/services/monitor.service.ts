import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
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

  getMonitorById(id: string): Observable<ProductMonitorResponse> {
    return this.http.get<ProductMonitorResponse>(`${this.apiUrl}/${id}`);
  }

  createMonitor(monitor: ProductMonitorRequest): Observable<ProductMonitorResponse> {
    return this.http.post<ProductMonitorResponse>(this.apiUrl, monitor);
  }

  updateMonitor(id: string, monitor: ProductMonitorRequest): Observable<ProductMonitorResponse> {
    return this.http.put<ProductMonitorResponse>(`${this.apiUrl}/${id}`, monitor);
  }

  deactivateMonitor(id: string): Observable<any> {
    return this.http.patch(`${this.apiUrl}/${id}/deactivate`, null, { responseType: 'text' });
  }

  activateMonitor(id: string): Observable<any> {
    return this.http.patch(`${this.apiUrl}/${id}/activate`, null, { responseType: 'text' });
  }

  deleteMonitor(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`, { responseType: 'text' });
  }

  getAllListings(page = 0, size = 20, filters?: any): Observable<PageResponse<ScrapedListingResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters) {
      Object.keys(filters).forEach(key => {
        if (filters[key] !== null && filters[key] !== undefined && filters[key] !== '') {
          params = params.set(key, filters[key]);
        }
      });
    }
    return this.http.get<PageResponse<ScrapedListingResponse>>(`${this.apiUrl}/listings`, { params });
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
