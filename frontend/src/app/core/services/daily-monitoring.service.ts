import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import {
  DailyLogResponse,
  NutritionEntryRequest, NutritionEntryResponse,
  MedicationIntakeLogRequest, MedicationIntakeLogResponse,
  ActivityEntryRequest, ActivityEntryResponse,
  IncidentEntryRequest, IncidentEntryResponse,
  AvailableMedication,
} from '@/core/models/daily-monitoring.model';

@Injectable({ providedIn: 'root' })
export class DailyMonitoringService {
  private readonly base = `${environment.apiBaseUrl}/api/daily-monitoring`;

  constructor(private readonly http: HttpClient) {}

  // ── Daily log ──────────────────────────────────────────────────────────
  getOrCreateLogForDate(keycloakId: string, date: string): Observable<DailyLogResponse> {
    return this.http.post<DailyLogResponse>(`${this.base}/patient/${keycloakId}/date/${date}`, {});
  }
  getLogsForPatient(keycloakId: string): Observable<DailyLogResponse[]> {
    return this.http.get<DailyLogResponse[]>(`${this.base}/patient/${keycloakId}`);
  }
  getLogById(id: number): Observable<DailyLogResponse> {
    return this.http.get<DailyLogResponse>(`${this.base}/${id}`);
  }

  // ── Available medications ──────────────────────────────────────────────
  getAvailableMedications(keycloakId: string): Observable<AvailableMedication[]> {
    return this.http.get<AvailableMedication[]>(`${this.base}/patient/${keycloakId}/available-medications`);
  }

  // ── Nutrition ──────────────────────────────────────────────────────────
  addNutrition(logId: number, dto: NutritionEntryRequest): Observable<NutritionEntryResponse> {
    return this.http.post<NutritionEntryResponse>(`${this.base}/${logId}/nutrition`, dto);
  }
  updateNutrition(logId: number, id: number, dto: NutritionEntryRequest): Observable<NutritionEntryResponse> {
    return this.http.put<NutritionEntryResponse>(`${this.base}/${logId}/nutrition/${id}`, dto);
  }
  deleteNutrition(logId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${logId}/nutrition/${id}`);
  }

  // ── Medication intakes ─────────────────────────────────────────────────
  addMedicationIntake(logId: number, dto: MedicationIntakeLogRequest): Observable<MedicationIntakeLogResponse> {
    return this.http.post<MedicationIntakeLogResponse>(`${this.base}/${logId}/medication-intakes`, dto);
  }
  updateMedicationIntake(logId: number, id: number, dto: MedicationIntakeLogRequest): Observable<MedicationIntakeLogResponse> {
    return this.http.put<MedicationIntakeLogResponse>(`${this.base}/${logId}/medication-intakes/${id}`, dto);
  }
  deleteMedicationIntake(logId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${logId}/medication-intakes/${id}`);
  }

  // ── Activities ─────────────────────────────────────────────────────────
  addActivity(logId: number, dto: ActivityEntryRequest): Observable<ActivityEntryResponse> {
    return this.http.post<ActivityEntryResponse>(`${this.base}/${logId}/activities`, dto);
  }
  updateActivity(logId: number, id: number, dto: ActivityEntryRequest): Observable<ActivityEntryResponse> {
    return this.http.put<ActivityEntryResponse>(`${this.base}/${logId}/activities/${id}`, dto);
  }
  deleteActivity(logId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${logId}/activities/${id}`);
  }

  // ── Voice note ────────────────────────────────────────────────────────────
  uploadVoiceNote(logId: number, formData: FormData): Observable<{ text: string }> {
    return this.http.post<{ text: string }>(`${this.base}/${logId}/voice-note`, formData);
  }
  deleteVoiceNote(logId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${logId}/voice-note`);
  }

  // ── Incidents ──────────────────────────────────────────────────────────
  addIncident(logId: number, dto: IncidentEntryRequest): Observable<IncidentEntryResponse> {
    return this.http.post<IncidentEntryResponse>(`${this.base}/${logId}/incidents`, dto);
  }
  updateIncident(logId: number, id: number, dto: IncidentEntryRequest): Observable<IncidentEntryResponse> {
    return this.http.put<IncidentEntryResponse>(`${this.base}/${logId}/incidents/${id}`, dto);
  }
  deleteIncident(logId: number, id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${logId}/incidents/${id}`);
  }
}
