// src/app/core/services/prescription.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PrescriptionRequestDTO,
  PrescriptionResponseDTO,
} from '@/core/models/prescription.model';

@Injectable({
  providedIn: 'root',
})
export class PrescriptionService {
  private readonly baseUrl = 'http://localhost:9090/api/prescriptions';

  constructor(private readonly http: HttpClient) {}

  getAllPrescriptions(): Observable<PrescriptionResponseDTO[]> {
    return this.http.get<PrescriptionResponseDTO[]>(this.baseUrl);
  }

  getPrescriptionById(id: number): Observable<PrescriptionResponseDTO> {
    return this.http.get<PrescriptionResponseDTO>(`${this.baseUrl}/${id}`);
  }

  getPrescriptionsBySession(sessionId: number): Observable<PrescriptionResponseDTO[]> {
    return this.http.get<PrescriptionResponseDTO[]>(`${this.baseUrl}/session/${sessionId}`);
  }

  getPrescriptionsByPatient(patientId: string): Observable<PrescriptionResponseDTO[]> {
    return this.http.get<PrescriptionResponseDTO[]>(`${this.baseUrl}/patient/${patientId}`);
  }

  createPrescription(
    prescription: PrescriptionRequestDTO
  ): Observable<PrescriptionResponseDTO> {
    return this.http.post<PrescriptionResponseDTO>(this.baseUrl, prescription);
  }

  updatePrescription(
    id: number,
    prescription: PrescriptionRequestDTO
  ): Observable<PrescriptionResponseDTO> {
    return this.http.put<PrescriptionResponseDTO>(
      `${this.baseUrl}/${id}`,
      prescription
    );
  }

  deletePrescription(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
