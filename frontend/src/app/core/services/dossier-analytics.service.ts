import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '@/environments/environment';

const BASE = `${environment.apiBaseUrl}/api/medical-folders/analytics`;

export interface DiseaseCount {
  diseaseName: string;
  count: number;
}

export interface DiagnosticsByMonth {
  year: number;
  month: number;
  diseaseName: string;
  count: number;
}

export interface MonthComparison {
  thisMonthDiagnostics: number;
  lastMonthDiagnostics: number;
  thisMonthFolders: number;
  lastMonthFolders: number;
}

export interface CrossPatientDisease {
  diagnosticsId: number;
  medicalFolderId: number;
  patientId: string;
  patientDisplayName?: string | null;
  doctorId: string;
  doctorDisplayName?: string | null;
  diseaseName: string;
  stage: string | null;
  diagnosisDate: string;
}

export interface ClinicalSafetyStats {
  treatmentCoverageRate: number;
  polypharmacyRiskCount: number;
  chronicMonitoringAlerts: number;
  potentialConflicts: {
    patientId: string;
    patientDisplayName?: string | null;
    medicationName: string;
    conflictingCondition: string;
    severity: string;
  }[];
  /** Backend filled illustrative KPIs when tracking has no prescription data (presentation-demo). */
  illustrationData?: boolean;
  /** Google Gemini augmented chronic alerts / conflicts. */
  geminiEnriched?: boolean;
  /** Short status from backend (API key, errors). */
  geminiNote?: string | null;
}

export interface FlaggedPatient {
  medicalFolderId: number;
  patientId: string;
  patientDisplayName?: string | null;
  consecutiveNoShows: number;
  attendanceRiskLevel: string;
  bookingRestricted: boolean;
  manualReviewRequired: boolean;
  restrictionReason?: string | null;
}

@Injectable({ providedIn: 'root' })
export class DossierAnalyticsService {
  constructor(private readonly http: HttpClient) { }

  getByDisease(diseaseName: string, stage?: string | null): Observable<CrossPatientDisease[]> {
    const params: Record<string, string> = { diseaseName: diseaseName.trim() };
    if (stage != null && stage.trim() !== '') params['stage'] = stage.trim();
    return this.http.get<CrossPatientDisease[]>(`${BASE}/by-disease`, { params });
  }

  getTopDiseases(limit = 10): Observable<DiseaseCount[]> {
    return this.http.get<DiseaseCount[]>(`${BASE}/top-diseases`, {
      params: { limit: String(limit) },
    });
  }

  getDiagnosticsByMonth(year?: number): Observable<DiagnosticsByMonth[]> {
    const options = year != null ? { params: { year: String(year) } } : {};
    return this.http.get<DiagnosticsByMonth[]>(`${BASE}/by-month`, options);
  }

  getMonthComparison(): Observable<MonthComparison> {
    return this.http.get<MonthComparison>(`${BASE}/comparison`);
  }

  getSafetyAudit(): Observable<ClinicalSafetyStats> {
    return this.http.get<ClinicalSafetyStats>(`${BASE}/safety-audit`);
  }

  getFlaggedPatients(): Observable<FlaggedPatient[]> {
    return this.http.get<FlaggedPatient[]>(`${BASE}/flagged-patients`).pipe(
      catchError(() => of([]))
    );
  }

  getFolderInsights(folderId: number): Observable<FolderInsights> {
    return this.http.get<FolderInsights>(`${BASE}/folder/${folderId}`);
  }
}

export interface FolderInsights {
  totalDiagnostics: number;
  totalMedicalHistory: number;
  treatmentCoverageRate: number;
  severityDistribution: Record<string, number>;
  prescriptions: { medicationName: string; prescribedAt: string }[];
  timeline: { date: string; diseaseName: string; stage: string }[];
}
