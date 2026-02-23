import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface MedicalFolder {
  id: number;
  patientId: string;
  doctorId: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateMedicalFolderRequest {
  patientId: string;
  doctorId?: string;
}

export interface UpdateMedicalFolderRequest {
  patientId?: string;
  doctorId?: string;
}

/** @deprecated Use MedicalFolder */
export type MedicalFolderResponseDTO = MedicalFolder;

@Injectable({
  providedIn: 'root',
})
export class MedicalFolderService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/medical-folders`;

  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<MedicalFolder[]> {
    return this.http.get<MedicalFolder[]>(this.baseUrl);
  }

  getById(id: number): Observable<MedicalFolder> {
    return this.http.get<MedicalFolder>(`${this.baseUrl}/${id}`);
  }

  create(data: CreateMedicalFolderRequest): Observable<MedicalFolder> {
    return this.http.post<MedicalFolder>(this.baseUrl, data);
  }

  update(id: number, data: UpdateMedicalFolderRequest): Observable<MedicalFolder> {
    return this.http.put<MedicalFolder>(`${this.baseUrl}/${id}`, data);
  }

  patch(id: number, data: UpdateMedicalFolderRequest): Observable<MedicalFolder> {
    return this.http.patch<MedicalFolder>(`${this.baseUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  /** Get folders by patient (if backend supports it) */
  getMedicalFoldersByPatient(patientId: string): Observable<MedicalFolder[]> {
    return this.http.get<MedicalFolder[]>(`${this.baseUrl}/patient/${patientId}`);
  }

  /** Get folders for a specific doctor */
  getByDoctorId(doctorId: string): Observable<MedicalFolder[]> {
    return this.http.get<MedicalFolder[]>(`${this.baseUrl}/doctor/${doctorId}`);
  }
}
