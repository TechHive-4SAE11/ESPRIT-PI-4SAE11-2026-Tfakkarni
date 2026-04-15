import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import type {
  PatientScoreResponse,
  ScoreHistoryEntry,
  CognitiveDomainDTO,
  DoctorEffectivenessResponse,
  PlatformOverviewResponse,
  BatchJobResult,
  CorrelationStatsResponse,
  PrescriptionImpactResponse,
  DoctorMatchResponse,
  SeverePatientResponse,
} from '@/core/models/analytics.model';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly base = `${environment.apiBaseUrl}/api/analytics`;

  constructor(private readonly http: HttpClient) {}

  // ─── Patient Score ───

  getPatientScore(keycloakId: string): Observable<PatientScoreResponse> {
    return this.http.get<PatientScoreResponse>(
      `${this.base}/patient/${encodeURIComponent(keycloakId)}/score`
    );
  }

  recomputePatientScore(keycloakId: string): Observable<PatientScoreResponse> {
    return this.http.post<PatientScoreResponse>(
      `${this.base}/patient/${encodeURIComponent(keycloakId)}/score/recompute`,
      {}
    );
  }

  getScoreHistory(keycloakId: string, days = 90): Observable<ScoreHistoryEntry[]> {
    const params = new HttpParams().set('days', String(days));
    return this.http.get<ScoreHistoryEntry[]>(
      `${this.base}/patient/${encodeURIComponent(keycloakId)}/score/history`,
      { params }
    );
  }

  getCognitiveDomains(keycloakId: string): Observable<CognitiveDomainDTO[]> {
    return this.http.get<CognitiveDomainDTO[]>(
      `${this.base}/patient/${encodeURIComponent(keycloakId)}/cognitive-domains`
    );
  }

  getCorrelationStats(keycloakId: string, days = 30): Observable<CorrelationStatsResponse> {
    const params = new HttpParams().set('days', String(days));
    return this.http.get<CorrelationStatsResponse>(
      `${this.base}/patient/${encodeURIComponent(keycloakId)}/correlation`,
      { params }
    );
  }

  getPrescriptionImpact(keycloakId: string, days = 60): Observable<PrescriptionImpactResponse> {
    const params = new HttpParams().set('days', String(days));
    return this.http.get<PrescriptionImpactResponse>(
      `${this.base}/patient/${encodeURIComponent(keycloakId)}/prescription-impact`,
      { params }
    );
  }

  // ─── Doctor Effectiveness ───

  getDoctorEffectiveness(keycloakId: string): Observable<DoctorEffectivenessResponse> {
    return this.http.get<DoctorEffectivenessResponse>(
      `${this.base}/doctor/${encodeURIComponent(keycloakId)}/effectiveness`
    );
  }

  recomputeDoctorEffectiveness(keycloakId: string): Observable<DoctorEffectivenessResponse> {
    return this.http.post<DoctorEffectivenessResponse>(
      `${this.base}/doctor/${encodeURIComponent(keycloakId)}/effectiveness/recompute`,
      {}
    );
  }

  getDoctorRanking(): Observable<DoctorEffectivenessResponse[]> {
    return this.http.get<DoctorEffectivenessResponse[]>(
      `${this.base}/doctor/ranking`
    );
  }

  getDoctorRedFlags(): Observable<DoctorEffectivenessResponse[]> {
    return this.http.get<DoctorEffectivenessResponse[]>(
      `${this.base}/doctor/red-flags`
    );
  }

  // ─── Platform Overview ───

  getPlatformOverview(): Observable<PlatformOverviewResponse> {
    return this.http.get<PlatformOverviewResponse>(
      `${this.base}/platform/overview`
    );
  }

  // ─── Batch Jobs ───

  runAllJobs(): Observable<BatchJobResult> {
    return this.http.post<BatchJobResult>(`${this.base}/jobs/run-all`, {});
  }

  runPatientScores(): Observable<BatchJobResult> {
    return this.http.post<BatchJobResult>(`${this.base}/jobs/patient-scores`, {});
  }

  runDoctorEffectiveness(): Observable<BatchJobResult> {
    return this.http.post<BatchJobResult>(`${this.base}/jobs/doctor-effectiveness`, {});
  }

  // ─── Doctor–Patient Matching ───

  getRankedDoctors(): Observable<DoctorMatchResponse[]> {
    return this.http.get<DoctorMatchResponse[]>(`${this.base}/matching/ranked-doctors`);
  }

  getRecommendedDoctor(stage: string = 'SEVERE'): Observable<DoctorMatchResponse> {
    const params = new HttpParams().set('stage', stage);
    return this.http.get<DoctorMatchResponse>(`${this.base}/matching/recommend`, { params });
  }

  getSeverePatients(): Observable<SeverePatientResponse[]> {
    return this.http.get<SeverePatientResponse[]>(`${this.base}/matching/severe-patients`);
  }
}
