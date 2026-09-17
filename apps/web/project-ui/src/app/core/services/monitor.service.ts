import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';
import { ProductMonitorRequest, ProductMonitorResponse, PageResponse, ScraperQueueStatusResponse, ScrapingFrequencyOption } from '../models/monitor.model';
import { ScrapedListingResponse } from '../models/listing.model';

@Injectable({
  providedIn: 'root'
})
export class MonitorService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/product-monitors`;

  getFrequencies(): Observable<ScrapingFrequencyOption[]> {
    return this.http.get<ScrapingFrequencyOption[]>(`${this.apiUrl}/frequencies`);
  }

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

  deactivateMonitor(id: string): Observable<string> {
    return this.http.patch<string>(`${this.apiUrl}/${id}/deactivate`, null, { responseType: 'text' });
  }

  activateMonitor(id: string): Observable<string> {
    return this.http.patch<string>(`${this.apiUrl}/${id}/activate`, null, { responseType: 'text' });
  }

  deleteMonitor(id: string): Observable<string> {
    return this.http.delete(`${this.apiUrl}/${id}`, { responseType: 'text' });
  }

  getAllListings(page = 0, size = 20, filters?: Record<string, string | number | boolean>): Observable<PageResponse<ScrapedListingResponse>> {
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

  getMonitorListings(
    monitorId: string,
    page = 0,
    size = 20,
    filters?: { q?: string; tier?: string; deliveryOnly?: boolean; sort?: string },
  ): Observable<PageResponse<ScrapedListingResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters?.q?.trim()) params = params.set('q', filters.q.trim());
    if (filters?.tier) params = params.set('tier', filters.tier);
    if (filters?.deliveryOnly) params = params.set('deliveryOnly', true);
    if (filters?.sort) params = params.set('sort', filters.sort);
    return this.http.get<PageResponse<ScrapedListingResponse>>(`${this.apiUrl}/${monitorId}/listings`, { params });
  }

  getScraperQueueStatus(): Observable<ScraperQueueStatusResponse> {
    return this.http.get<ScraperQueueStatusResponse>(`${environment.apiUrl}/scraper/queue-status`);
  }
}
