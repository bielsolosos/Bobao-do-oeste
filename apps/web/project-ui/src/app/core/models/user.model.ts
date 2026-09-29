import { UserConfig } from './user-config.model';

export interface UserResponse {
  id: string;
  username: string;
  email?: string;
  active: boolean;
  roles: string[];
  config?: UserConfig;
}
