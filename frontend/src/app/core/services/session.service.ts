// src/app/core/services/session.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SessionRequestDTO {
  medicalFolderId: number;
  sessionDate: string;
  notes: string;
}

export interface SessionResponseDTO {
  id: number;
  medicalFolderId: number;
  sessionDate: string;
  notes: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root',
})
export class SessionService {
  private readonly baseUrl = 'http://localhost:9090/api/sessions';

  constructor(private readonly http: HttpClient) {}

  getSessionsByMedicalFolder(medicalFolderId: number): Observable<SessionResponseDTO[]> {
    return this.http.get<SessionResponseDTO[]>(`${this.baseUrl}/medical-folder/${medicalFolderId}`);
  }

  createSession(session: SessionRequestDTO): Observable<SessionResponseDTO> {
    return this.http.post<SessionResponseDTO>(this.baseUrl, session);
  }
}
