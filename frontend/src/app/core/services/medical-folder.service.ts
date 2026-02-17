// src/app/core/services/medical-folder.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface MedicalFolderResponseDTO {
  id: number;
  idPatient: string;
  idDoctor: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root',
})
export class MedicalFolderService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/medical-folders`;

  constructor(private readonly http: HttpClient) { }

  getMedicalFoldersByPatient(patientId: string): Observable<MedicalFolderResponseDTO[]> {
    return this.http.get<MedicalFolderResponseDTO[]>(`${this.baseUrl}/patient/${patientId}`);
  }
}
