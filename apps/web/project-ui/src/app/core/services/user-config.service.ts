import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AvailableAiModelsResponse, UpdateUserConfigRequest, UserConfig } from '../models/user-config.model';

@Injectable({
  providedIn: 'root',
})
export class UserConfigService {
  private http = inject(HttpClient);

  getAvailableModels(): Observable<AvailableAiModelsResponse> {
    return this.http.get<AvailableAiModelsResponse>(`${environment.apiUrl}/me/configs/models`);
  }

  updateConfig(request: UpdateUserConfigRequest): Observable<UserConfig> {
    return this.http.put<UserConfig>(`${environment.apiUrl}/me/configs`, request);
  }
}
