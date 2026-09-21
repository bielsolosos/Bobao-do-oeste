import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';
import {
  MetricsOverviewResponse,
  TierMetricsResponse,
  PriceDistributionResponse,
  BrandDistributionResponse,
  TimelineMetricsResponse,
} from '../models/metrics.model';

@Injectable({
  providedIn: 'root',
})
export class MonitorMetricsService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/product-monitors/metrics`;

  getOverview(monitorId?: string): Observable<MetricsOverviewResponse> {
    let params = new HttpParams();
    if (monitorId) {
      params = params.set('monitorId', monitorId);
    }
    return this.http.get<MetricsOverviewResponse>(`${this.apiUrl}/overview`, { params });
  }

  getTiers(monitorId?: string): Observable<TierMetricsResponse> {
    let params = new HttpParams();
    if (monitorId) {
      params = params.set('monitorId', monitorId);
    }
    return this.http.get<TierMetricsResponse>(`${this.apiUrl}/tiers`, { params });
  }

  getPrices(monitorId?: string): Observable<PriceDistributionResponse> {
    let params = new HttpParams();
    if (monitorId) {
      params = params.set('monitorId', monitorId);
    }
    return this.http.get<PriceDistributionResponse>(`${this.apiUrl}/prices`, { params });
  }

  getBrands(monitorId?: string): Observable<BrandDistributionResponse> {
    let params = new HttpParams();
    if (monitorId) {
      params = params.set('monitorId', monitorId);
    }
    return this.http.get<BrandDistributionResponse>(`${this.apiUrl}/brands`, { params });
  }

  getTimeline(monitorId?: string, days = 14): Observable<TimelineMetricsResponse> {
    let params = new HttpParams().set('days', days.toString());
    if (monitorId) {
      params = params.set('monitorId', monitorId);
    }
    return this.http.get<TimelineMetricsResponse>(`${this.apiUrl}/timeline`, { params });
  }
}
