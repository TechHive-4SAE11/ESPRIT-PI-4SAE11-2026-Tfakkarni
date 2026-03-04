import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface Diagnostics {
  id: number;
  medicalFolderId: number;
  diseaseName: string;
  stage: string;
  comorbidities: string;
  diagnosisDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDiagnosticsRequest {
  medicalFolderId: number;
  diseaseName: string;
  stage?: string;
  comorbidities?: string;
  diagnosisDate: string;
}

export interface UpdateDiagnosticsRequest {
  diseaseName?: string;
  stage?: string;
  comorbidities?: string;
  diagnosisDate?: string;
}

@Injectable({
  providedIn: 'root',
})
export class DiagnosticsService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/diagnostics`;

  constructor(private readonly http: HttpClient) {}

  getByFolder(medicalFolderId: number): Observable<Diagnostics[]> {
    return this.http.get<Diagnostics[]>(this.baseUrl, {
      params: { medicalFolderId: String(medicalFolderId) },
    });
  }

  getById(id: number): Observable<Diagnostics> {
    return this.http.get<Diagnostics>(`${this.baseUrl}/${id}`);
  }

  create(data: CreateDiagnosticsRequest): Observable<Diagnostics> {
    return this.http.post<Diagnostics>(this.baseUrl, data);
  }

  update(id: number, data: UpdateDiagnosticsRequest): Observable<Diagnostics> {
    return this.http.put<Diagnostics>(`${this.baseUrl}/${id}`, data);
  }

  patch(id: number, data: UpdateDiagnosticsRequest): Observable<Diagnostics> {
    return this.http.patch<Diagnostics>(`${this.baseUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
