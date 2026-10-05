import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AcceptInviteRequest,
  CreateInviteRequest,
  PageResponse,
  UserInviteResponse,
  ValidateInviteResponse,
} from '../models/user-invite.model';
import { AuthService, LoginResponse } from './auth.service';

@Injectable({
  providedIn: 'root',
})
export class InviteService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);

  // Endpoints Públicos
  validateInvite(token: string): Observable<ValidateInviteResponse> {
    const params = new HttpParams().set('token', token);
    return this.http.get<ValidateInviteResponse>(`${environment.apiUrl}/auth/invites/validate`, {
      params,
    });
  }

  acceptInvite(request: AcceptInviteRequest, redirectTo: string = '/dashboard'): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiUrl}/auth/invites/accept`, request).pipe(
      tap((response) => {
        this.authService.handleAuthSuccess(response, redirectTo);
      }),
    );
  }

  // Endpoints Administrativos
  listInvites(page: number = 0, size: number = 10): Observable<PageResponse<UserInviteResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<UserInviteResponse>>(`${environment.apiUrl}/admin/invites`, {
      params,
    });
  }

  createInvite(request: CreateInviteRequest): Observable<UserInviteResponse> {
    return this.http.post<UserInviteResponse>(`${environment.apiUrl}/admin/invites`, request);
  }

  cancelInvite(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/admin/invites/${id}`);
  }

  resendInvite(id: string): Observable<UserInviteResponse> {
    return this.http.post<UserInviteResponse>(`${environment.apiUrl}/admin/invites/${id}/resend`, {});
  }
}
