import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface MedicalHistory {
  id: number;
  medicalFolderId: number;
  allergies: string;
  conditions: string;
  surgeries: string;
  symptoms?: string;
  recommendedTreatment?: string;
  familyHistory?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateMedicalHistoryRequest {
  medicalFolderId: number;
  allergies?: string;
  conditions?: string;
  surgeries?: string;
  symptoms?: string;
  recommendedTreatment?: string;
  familyHistory?: string;
}

export interface UpdateMedicalHistoryRequest {
  allergies?: string;
  conditions?: string;
  surgeries?: string;
  symptoms?: string;
  recommendedTreatment?: string;
  familyHistory?: string;
}

@Injectable({
  providedIn: 'root',
})
export class MedicalHistoryService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/medical-history`;

  constructor(private readonly http: HttpClient) {}

  getByFolder(medicalFolderId: number): Observable<MedicalHistory[]> {
    return this.http.get<MedicalHistory[]>(this.baseUrl, {
      params: { medicalFolderId: String(medicalFolderId) },
    });
  }

  getById(id: number): Observable<MedicalHistory> {
    return this.http.get<MedicalHistory>(`${this.baseUrl}/${id}`);
  }

  create(data: CreateMedicalHistoryRequest): Observable<MedicalHistory> {
    return this.http.post<MedicalHistory>(this.baseUrl, data);
  }

  update(id: number, data: UpdateMedicalHistoryRequest): Observable<MedicalHistory> {
    return this.http.put<MedicalHistory>(`${this.baseUrl}/${id}`, data);
  }

  patch(id: number, data: UpdateMedicalHistoryRequest): Observable<MedicalHistory> {
    return this.http.patch<MedicalHistory>(`${this.baseUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
