import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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
  private readonly baseUrl = 'http://localhost:9090/api/users';

  constructor(private readonly http: HttpClient) {}

  getAllUsers(): Observable<UserInfo[]> {
    return this.http.get<UserInfo[]>(this.baseUrl);
  }

  getUsersByRole(role: string): Observable<UserInfo[]> {
    return this.http.get<UserInfo[]>(`${this.baseUrl}/role/${role}`);
  }

  getUserByKeycloakId(keycloakId: string): Observable<UserInfo> {
    return this.http.get<UserInfo>(`${this.baseUrl}/keycloak/${keycloakId}`);
  }
}
