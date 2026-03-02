import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface UserInfo {
  id: number;
  keycloakId: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  phone?: string;
  enabled: boolean;
  createdAt: string;
}

@Injectable({
  providedIn: 'root',
})
export class UserApiService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/users`;

  constructor(private readonly http: HttpClient) { }

  getAllUsers(): Observable<UserInfo[]> {
    return this.http.get<UserInfo[]>(this.baseUrl);
  }

  getUsersByRole(role: string): Observable<UserInfo[]> {
    return this.http.get<UserInfo[]>(`${this.baseUrl}/role/${role}`);
  }

  getUserByKeycloakId(keycloakId: string): Observable<UserInfo> {
    return this.http.get<UserInfo>(`${this.baseUrl}/keycloak/${keycloakId}`);
  }

  getUserById(id: string | number): Observable<UserInfo> {
    return this.http.get<UserInfo>(`${this.baseUrl}/${id}`);
  }

  updateProfile(keycloakId: string, data: { firstName: string; lastName: string; email: string; phone?: string }): Observable<UserInfo> {
    return this.http.put<UserInfo>(`${this.baseUrl}/profile/${keycloakId}`, data);
  }

  changePassword(keycloakId: string, data: { currentPassword: string; newPassword: string }): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/password/${keycloakId}`, data);
  }

  adminResetPassword(keycloakId: string, newPassword: string): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/admin-reset-password/${keycloakId}`, { newPassword });
  }

  registerUser(data: { firstName: string; lastName: string; email: string; password: string; role: string }): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/register`, data);
  }

  deleteUser(keycloakId: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.baseUrl}/keycloak/${keycloakId}`);
  }

  updateRole(keycloakId: string, role: string): Observable<UserInfo> {
    return this.http.put<UserInfo>(`${this.baseUrl}/role/${keycloakId}`, { role });
  }

  toggleEnabled(keycloakId: string, enabled: boolean): Observable<UserInfo> {
    return this.http.put<UserInfo>(`${this.baseUrl}/toggle-enabled/${keycloakId}`, { enabled });
  }

  forgotPassword(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl.replace('/api/users', '/api/password-reset')}/forgot`, { email });
  }

  verifyAndResetPassword(email: string, code: string, newPassword: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl.replace('/api/users', '/api/password-reset')}/verify`, { email, code, newPassword });
  }
}
