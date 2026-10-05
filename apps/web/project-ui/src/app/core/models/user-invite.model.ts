export type UserInviteStatus = 'PENDING' | 'ACCEPTED' | 'EXPIRED' | 'CANCELLED';

export interface UserInviteResponse {
  id: string;
  email: string;
  role: string;
  status: UserInviteStatus;
  expiresAt: string;
  acceptedAt?: string;
  createdAt: string;
  invitedByUsername?: string;
  acceptedByUsername?: string;
}

export interface CreateInviteRequest {
  email: string;
  role?: string;
}

export interface ValidateInviteResponse {
  email: string;
  valid: boolean;
  expiresAt: string;
}

export interface AcceptInviteRequest {
  token: string;
  username: string;
  password: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
