import { UserConfig } from './user-config.model';

export interface UserResponse {
  id: string;
  username: string;
  email?: string;
  active: boolean;
  roles: string[];
  config?: UserConfig;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  oldPasswordConfirmation: string;
  newPassword: string;
}

export interface EditUserRequest {
  username: string;
  email: string;
}

export interface MessageResponse {
  Message?: string;
  message?: string;
}

