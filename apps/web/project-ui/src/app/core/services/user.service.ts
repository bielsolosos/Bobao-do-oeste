import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ChangePasswordRequest,
  EditUserRequest,
  MessageResponse,
  UserResponse,
} from '../models/user.model';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private http = inject(HttpClient);

  changePassword(userId: string, request: ChangePasswordRequest): Observable<MessageResponse> {
    return this.http.put<MessageResponse>(
      `${environment.apiUrl}/me/change-password/${userId}`,
      request,
    );
  }

  editCredentials(request: EditUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${environment.apiUrl}/me/edit-credentials`, request);
  }
}
