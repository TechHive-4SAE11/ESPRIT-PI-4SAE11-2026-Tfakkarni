// src/app/core/services/prescription.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import {
  PrescriptionRequestDTO,
  PrescriptionResponseDTO,
} from '@/core/models/prescription.model';
import { PagedResponse } from '@/core/models/paged-response.model';

@Injectable({
  providedIn: 'root',
})
export class PrescriptionService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/prescriptions`;

  constructor(private readonly http: HttpClient) { }

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
  
  getPrescriptionsByPatientPaginated(
    patientId: string, 
    page: number = 0, 
    size: number = 10,
    sortBy: string = 'createdAt',
    sortDir: 'ASC' | 'DESC' = 'DESC'
  ): Observable<PagedResponse<PrescriptionResponseDTO>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    
    return this.http.get<PagedResponse<PrescriptionResponseDTO>>(
      `${this.baseUrl}/patient/${patientId}/paginated`,
      { params }
    );
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

  downloadPrescriptionPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${id}/pdf`, { responseType: 'blob' });
  }
}
