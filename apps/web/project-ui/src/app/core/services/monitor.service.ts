import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';
import { ProductMonitorRequest, ProductMonitorResponse, PageResponse } from '../models/monitor.model';

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
}
