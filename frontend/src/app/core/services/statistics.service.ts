import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import type {
  ScoreTrendResponse,
  IncidentStatsResponse,
  MedicationComplianceResponse,
  HydrationTrendResponse,
  ActivityTrendResponse,
  StreakResponse,
} from '@/core/models/statistics.model';

export type PeriodMode =
  | { type: 'days'; value: number }           // fenêtre glissante : 7 ou 30 j
  | { type: 'current_month' }                 // mois courant complet
  | { type: 'previous_month' }                // mois précédent complet
  | { type: 'range'; start: string; end: string }; // dates ISO

@Injectable({ providedIn: 'root' })
export class StatisticsService {
  private readonly base = `${environment.apiBaseUrl}/api/statistics`;

  constructor(private readonly http: HttpClient) {}

  private params(mode: PeriodMode): HttpParams {
    if (mode.type === 'days') {
      return new HttpParams().set('days', String(mode.value));
    }
    if (mode.type === 'range') {
      return new HttpParams()
        .set('startDate', mode.start)
        .set('endDate', mode.end);
    }
    // current_month | previous_month
    return new HttpParams().set('period', mode.type);
  }

  getScoreTrend(patientId: string, mode: PeriodMode): Observable<ScoreTrendResponse> {
    return this.http.get<ScoreTrendResponse>(
      `${this.base}/${encodeURIComponent(patientId)}/score-trend`,
      { params: this.params(mode) }
    );
  }

  getIncidentTypes(patientId: string, mode: PeriodMode): Observable<IncidentStatsResponse> {
    return this.http.get<IncidentStatsResponse>(
      `${this.base}/${encodeURIComponent(patientId)}/incident-types`,
      { params: this.params(mode) }
    );
  }

  getMedicationCompliance(patientId: string, mode: PeriodMode): Observable<MedicationComplianceResponse> {
    return this.http.get<MedicationComplianceResponse>(
      `${this.base}/${encodeURIComponent(patientId)}/medication-compliance`,
      { params: this.params(mode) }
    );
  }

  getHydrationTrend(patientId: string, mode: PeriodMode): Observable<HydrationTrendResponse> {
    return this.http.get<HydrationTrendResponse>(
      `${this.base}/${encodeURIComponent(patientId)}/hydration-trend`,
      { params: this.params(mode) }
    );
  }

  getActivityTrend(patientId: string, mode: PeriodMode): Observable<ActivityTrendResponse> {
    return this.http.get<ActivityTrendResponse>(
      `${this.base}/${encodeURIComponent(patientId)}/activity-trend`,
      { params: this.params(mode) }
    );
  }

  /** Duolingo-style win streak based on daily health score >= 85. */
  getStreak(patientId: string): Observable<StreakResponse> {
    return this.http.get<StreakResponse>(
      `${this.base}/${encodeURIComponent(patientId)}/streak`
    );
  }
}
