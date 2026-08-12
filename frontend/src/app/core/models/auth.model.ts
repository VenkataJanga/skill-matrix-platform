export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userPublicId: string;
  username: string;
  fullName: string;
  roles: string[];
  mustChangePassword: boolean;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface UserMe {
  publicId: string;
  username: string;
  email: string;
  fullName: string;
  employeeId?: string;
  roles: string[];
  permissions: string[];
  mustChangePassword: boolean;
  lastLogin?: string;
}

export type UserRole = 'ADMIN' | 'LEAD_MANAGER' | 'TECHNICIAN';
