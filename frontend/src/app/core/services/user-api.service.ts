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
}
