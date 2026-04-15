import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';

export interface PatientBadge {
  id: number;
  patientId: string;
  badgeCode: string;
  badgeTitle: string;
  description: string;
  awardedAt: string;
  sourceGameType: string | null;
  sourceAttemptId: number | null;
}

/** Mapping badgeCode → emoji pour l'affichage */
export const BADGE_ICONS: Record<string, string> = {
  MEMORY_STAR: '🧠',
  THREE_DAY_STREAK: '🔄',
  IMPROVEMENT_BADGE: '📈',
  FAST_RECALL: '⚡',
  FOCUS_CHAMPION: '🎯',
  FIRST_GAME: '👣',
};

/** Mapping badgeCode → CSS color class */
export const BADGE_COLORS: Record<string, string> = {
  MEMORY_STAR: 'bg-violet-100 text-violet-700 border-violet-200',
  THREE_DAY_STREAK: 'bg-blue-100 text-blue-700 border-blue-200',
  IMPROVEMENT_BADGE: 'bg-emerald-100 text-emerald-700 border-emerald-200',
  FAST_RECALL: 'bg-amber-100 text-amber-700 border-amber-200',
  FOCUS_CHAMPION: 'bg-rose-100 text-rose-700 border-rose-200',
  FIRST_GAME: 'bg-orange-100 text-orange-700 border-orange-200',
};

@Injectable({
  providedIn: 'root',
})
export class PatientBadgeService {
  private readonly baseUrl = `${environment.apiBaseUrl}/api/patient-badges`;

  constructor(private readonly http: HttpClient) { }

  /** Récupère tous les badges d'un patient */
  getBadges(patientId: string): Observable<PatientBadge[]> {
    return this.http.get<PatientBadge[]>(`${this.baseUrl}/${patientId}`);
  }

  /** Déclenche l'évaluation et retourne les NOUVEAUX badges attribués */
  evaluateBadges(patientId: string): Observable<PatientBadge[]> {
    return this.http.post<PatientBadge[]>(`${this.baseUrl}/${patientId}/evaluate`, {});
  }
}
