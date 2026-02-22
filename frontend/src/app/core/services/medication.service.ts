import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { MedicationStatus } from '@/core/models/prescription.model';

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

@Injectable({
  providedIn: 'root'
})
export class MedicationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/medications`;

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
   * Get medication details
   */
  getMedication(medicationId: number): Observable<any> {
    return this.http.get(`${this.baseUrl}/${medicationId}`);
  }
}
