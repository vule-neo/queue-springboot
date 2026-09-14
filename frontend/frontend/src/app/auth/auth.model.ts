export interface AuthResponse {
  token: string;
  type: string;      // "Bearer"
  email: string;
  role: string;      // CUSTOMER | EMPLOYEE | MANAGER | ADMIN
}
