export interface User {
  id: number;
  email: string;
  name: string;
  createdAt?: Date;
}

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  name: string;
  expiresAt?: Date;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}
