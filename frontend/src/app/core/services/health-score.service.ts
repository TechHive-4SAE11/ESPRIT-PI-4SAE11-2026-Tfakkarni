import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@/environments/environment';
import { HealthScoreResponse } from '@/core/models/health-score.model';

@Injectable({ providedIn: 'root' })
export class HealthScoreService {
  private readonly base = `${environment.apiBaseUrl}/api/health-score`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Récupère le score santé quotidien pour un patient à une date donnée.
   * Aucune logique de calcul côté frontend — tout est fourni par l'API.
   */
  getDailyScore(patientKeycloakId: string, date: string): Observable<HealthScoreResponse> {
    const params = new HttpParams().set('date', date);
    return this.http.get<HealthScoreResponse>(
      `${this.base}/${encodeURIComponent(patientKeycloakId)}`,
      { params }
    );
  }
}
