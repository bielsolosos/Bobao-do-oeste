import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';
import { PageResponse } from '../models/monitor.model';
import { WebhookEventSummaryResponse } from '../models/webhook.model';

@Injectable({
  providedIn: 'root'
})
export class WebhookService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/webhooks`;

  getEvents(page = 0, size = 20): Observable<PageResponse<WebhookEventSummaryResponse>> {
    return this.http.get<PageResponse<WebhookEventSummaryResponse>>(`${this.apiUrl}/events?page=${page}&size=${size}`);
  }
}
