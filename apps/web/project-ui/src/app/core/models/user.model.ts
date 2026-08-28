export interface UserResponse {
  id: string;
  username: string;
  email?: string;
  active: boolean;
  roles: string[];
}
