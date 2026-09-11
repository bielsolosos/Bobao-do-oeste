import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';
import { WebhookEventSummaryResponse, WebhookStatus } from '../models/webhook.model';
import { PageResponse } from '../models/monitor.model'; // usando o mesmo PageResponse global

@Injectable({
  providedIn: 'root'
})
export class WebhookService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/webhooks`;

  getEvents(
    page = 0,
    size = 20,
    filters?: { status?: WebhookStatus; q?: string },
  ): Observable<PageResponse<WebhookEventSummaryResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters?.status) params = params.set('status', filters.status);
    if (filters?.q?.trim()) params = params.set('q', filters.q.trim());
    return this.http.get<PageResponse<WebhookEventSummaryResponse>>(`${this.apiUrl}/events`, { params });
  }
}
