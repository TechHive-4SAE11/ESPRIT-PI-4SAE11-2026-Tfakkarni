import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '@/environments/environment';

// ── Interfaces ────────────────────────────────────────────────────────────────

export interface CreateRatingRequest {
  meetingId: number;
  doctorKeycloakId: string;
  patientKeycloakId: string;
  rating: number;
  review?: string | null;
}

export interface DoctorRatingResponse {
  id: number;
  meetingId: number;
  doctorKeycloakId: string;
  patientKeycloakId: string;
  rating: number;
  review: string | null;
  doctorName: string;
  patientName: string;
  createdAt: string;
}

export interface DoctorRankingResponse {
  doctorKeycloakId: string;
  doctorName: string;
  averageRating: number;
  totalRatings: number;
  rank: number;
  recentReviews: DoctorRatingResponse[];
}

// ── Service ───────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class RatingService {
  private base = environment.apiBaseUrl + '/api/ratings';

  constructor(private http: HttpClient) {}

  /** Submit a rating — throws on error so callers can handle it */
  submitRating(req: CreateRatingRequest): Observable<DoctorRatingResponse> {
    return this.http.post<DoctorRatingResponse>(this.base, req);
    // No catchError here — let the component handle errors with its own logic
  }

  getRanking(): Observable<DoctorRankingResponse[]> {
    return this.http.get<DoctorRankingResponse[]>(`${this.base}/ranking`).pipe(
      catchError(() => of([]))
    );
  }

  getRatingsForDoctor(doctorKeycloakId: string): Observable<DoctorRatingResponse[]> {
    return this.http.get<DoctorRatingResponse[]>(`${this.base}/doctor/${doctorKeycloakId}`).pipe(
      catchError(() => of([]))
    );
  }

  checkRated(meetingId: number, patientKeycloakId: string): Observable<{ rated: boolean }> {
    return this.http.get<{ rated: boolean }>(
      `${this.base}/check`,
      { params: { meetingId: String(meetingId), patientKeycloakId } }
    ).pipe(catchError(() => of({ rated: false })));
  }
}
