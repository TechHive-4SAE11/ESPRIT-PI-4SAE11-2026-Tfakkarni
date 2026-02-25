import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { MedicationStatus, MedicationResponseDTO } from '@/core/models/prescription.model';
import { PagedResponse } from '@/core/models/paged-response.model';

export interface UpdateMedicationStatusRequest {
  status: MedicationStatus;
  reason?: string;
}

export interface UpdateMedicationStatusResponse {
  success: boolean;
  medicationId: number;
  oldStatus: MedicationStatus;
  newStatus: MedicationStatus;
  endDate: string | null;
  message: string;
}

export interface UpdateMedicationRequest {
  medicationName: string;
  dosage: string;
  frequency: string;
  duration: string;
  instructions?: string;
  startDate?: string;
  endDate?: string;
}

@Injectable({
  providedIn: 'root'
})
export class MedicationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/medications`;

  /**
   * Get paginated medications for a patient with optional status filter
   */
  getMedicationsByPatientPaginated(
    patientId: string,
    page: number = 0,
    size: number = 10,
    sortBy: string = 'createdAt',
    sortDir: string = 'DESC',
    status?: MedicationStatus
  ): Observable<PagedResponse<MedicationResponseDTO>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    
    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<PagedResponse<MedicationResponseDTO>>(
      `${this.baseUrl}/patient/${patientId}/paginated`,
      { params }
    );
  }

  /**
   * Get paginated medications for a doctor with optional status filter
   */
  getMedicationsByDoctorPaginated(
    doctorId: string,
    page: number = 0,
    size: number = 10,
    sortBy: string = 'createdAt',
    sortDir: string = 'DESC',
    status?: MedicationStatus
  ): Observable<PagedResponse<MedicationResponseDTO>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDir', sortDir);
    
    if (status) {
      params = params.set('status', status);
    }

    return this.http.get<PagedResponse<MedicationResponseDTO>>(
      `${this.baseUrl}/doctor/${doctorId}/paginated`,
      { params }
    );
  }

  /**
   * Update medication status (for doctors)
   */
  updateMedicationStatus(
    medicationId: number,
    request: UpdateMedicationStatusRequest
  ): Observable<UpdateMedicationStatusResponse> {
    return this.http.patch<UpdateMedicationStatusResponse>(
      `${this.baseUrl}/${medicationId}/status`,
      request
    );
  }

  /**
   * Update medication details
   */
  updateMedication(
    medicationId: number,
    request: UpdateMedicationRequest
  ): Observable<MedicationResponseDTO> {
    return this.http.put<MedicationResponseDTO>(
      `${this.baseUrl}/${medicationId}`,
      request
    );
  }

  /**
   * Get medication details
   */
  getMedication(medicationId: number): Observable<MedicationResponseDTO> {
    return this.http.get<MedicationResponseDTO>(`${this.baseUrl}/${medicationId}`);
  }
}
